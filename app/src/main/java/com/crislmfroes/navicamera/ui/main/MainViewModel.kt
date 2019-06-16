package com.crislmfroes.navicamera.ui.main

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beust.klaxon.*
import com.crislmfroes.navicamera.model.Marcador
import com.crislmfroes.navicamera.model.MarcadorDatabaseRoom
import com.crislmfroes.navicamera.model.MarcadorRemote
import com.crislmfroes.navicamera.model.MarcadorRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.aruco.Aruco
import org.opencv.aruco.CharucoBoard
import org.opencv.aruco.DetectorParameters
import org.opencv.aruco.Dictionary
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    private val markerDict : Dictionary
    private val board : CharucoBoard
    private val calibCorners = mutableListOf<Mat>()
    private val calibIds = mutableListOf<Mat>()
    private val minCalibFrames = 5
    private val fileDir = application.filesDir
    private var cameraMatrix : Mat? = null
    private var distCoeffs : Mat? = null
    private var isCalibrated = false
    private val detectorParameters : DetectorParameters
    val message = MutableLiveData<String>()
    private var isProcessing = false
    val overlay : MutableLiveData<Bitmap> = MutableLiveData()
    private val marcadorRepository : MarcadorRepository
    val marcadores = MutableLiveData<List<Marcador>>()

    init {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCv carregada com sucesso!")
        } else {
            Log.i(TAG, "Falha ao carregar OpenCv!")
        }
        val marcadorDao = MarcadorDatabaseRoom.getDatabase(application, viewModelScope).marcadorDao()
        val marcadorRemote = MarcadorRemote()
        marcadorRepository = MarcadorRepository(marcadorDao, marcadorRemote)
        detectorParameters = DetectorParameters.create()
        populateRoomDatabase()
        markerDict = loadDictionary("dictByteList.json")
        board = CharucoBoard.create(10, 10, 0.2f, 0.16f, markerDict)
        loadCameraParameters()
    }

    private fun populateRoomDatabase() = viewModelScope.launch {
        val remotes = marcadorRepository.allMarcadoresRemote.value
        remotes?.let {
            for (remote in it) {
                marcadorRepository.insert(remote)
            }
        }
    }

    private fun loadDictionary(filename : String) : Dictionary {
        val stream = getApplication<Application>().assets.open(filename)
        val parser = Parser.default()
        val json = parser.parse(stream) as JsonObject
        val dictionary = Aruco.custom_dictionary(1000, json["markerSize"] as Int)
        dictionary._maxCorrectionBits = json["maxCorrectionBits"] as Int
        val data = (json["bytesList"] as JsonObject)["data"] as JsonArray<Byte>
        dictionary._bytesList.put(0, 0, data.toByteArray())
        stream.close()
        return dictionary
    }

    private fun loadCameraParameters() {
        if (cameraMatrix == null || distCoeffs == null) {
            try {
                val parser = Parser.default()
                val file = File(fileDir, "cameraParameters.json")
                val json = parser.parse(file.absolutePath) as JsonObject
                val cameraJson = json["cameraMatrix"] as JsonObject
                val distJson = json["distCoeffs"] as JsonObject
                cameraMatrix = Mat(cameraJson["rows"] as Int, cameraJson["cols"] as Int, cameraJson["format"] as Int)
                distCoeffs = Mat(distJson["rows"] as Int, distJson["cols"] as Int, distJson["format"] as Int)
                cameraMatrix!!.put(0, 0, *(cameraJson["data"] as JsonArray<Double>).toDoubleArray())
                distCoeffs!!.put(0, 0, *(distJson["data"] as JsonArray<Double>).toDoubleArray())
                message.postValue("Detectando marcadores ...")
                isCalibrated = true
            } catch (e : FileNotFoundException) {
                isCalibrated = false
                cameraMatrix = Mat()
                distCoeffs = Mat()
                message.postValue("Calibrando camera 0/%d".format(minCalibFrames))
            }

        }
    }

    fun processImage(mat : Mat) {
        Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE)
        val marcadorList = mutableListOf<Marcador>()
        isProcessing = true
        if (!isCalibrated) {
            calibrateCamera(mat)
        } else {
            val corners = mutableListOf<Mat>()
            val ids = Mat()
            val rejected = mutableListOf<Mat>()
            Aruco.detectMarkers(mat, markerDict, corners, ids, detectorParameters, rejected)
            Aruco.drawDetectedMarkers(mat, corners, ids, Scalar(0.0, 0.0, 255.0))
            message.postValue("Detectados %d marcadores".format(corners.size))
            if (corners.size > 0) {
                val rvecs = Mat()
                val tvecs = Mat()
                Aruco.estimatePoseSingleMarkers(corners, 0.16f, cameraMatrix, distCoeffs, rvecs, tvecs)
                for (j in 0 until ids.total()) {
                    val rmat = Mat(3, 1, CvType.CV_32FC1)
                    val rdata = DoubleArray(3)
                    rvecs.get(0, j.toInt(), rdata)
                    rmat.put(0, 0, *rdata)
                    val tmat = Mat(3, 1, CvType.CV_32FC1)
                    val tdata = DoubleArray(3)
                    tvecs.get(0, j.toInt(), tdata)
                    tmat.put(0, 0, *tdata)
                    Calib3d.drawFrameAxes(mat, cameraMatrix, distCoeffs, rmat, tmat, 0.2f)
                    val id = ids[0, j.toInt()][0].toInt()
                    val marcador = marcadorRepository.get(id + 1)
                    marcador?.let {
                        val distancia = Core.norm(tmat)
                        val x = tdata[0]
                        val angle = sinh(x/distancia)
                        it.rotacao = angle.toFloat()
                        it.distancia = distancia.toFloat()
                        marcadorList.add(it)
                    }
                }
            }
        }
        isProcessing = false
        val bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        overlay.postValue(bitmap)
        marcadores.postValue(marcadorList)
    }

    private fun calibrateCamera(mat : Mat) {
        val corners = mutableListOf<Mat>()
        val ids = Mat()
        val rejected = mutableListOf<Mat>()
        Aruco.detectMarkers(mat, markerDict, corners, ids, detectorParameters, rejected)
        Aruco.refineDetectedMarkers(mat, board, corners, ids, rejected)
        Log.i(TAG, corners.toString())
        Log.i(TAG, ids.toString())
        if (ids.total() >= 4) {
            Log.i(TAG, "Marcadores detectados!")
            val charucoCorners = Mat()
            val charucoIds = Mat()
            Aruco.interpolateCornersCharuco(corners, ids, mat, board, charucoCorners, charucoIds)
            Log.i(TAG, charucoIds.toString())
            Log.i(TAG, charucoCorners.toString())
            if (charucoIds.total() > 0 && charucoCorners.rows() >= 4) {
                Log.i(TAG, "Placa detectada! %d cantos ...".format(charucoCorners.rows()))
                calibCorners.add(charucoCorners)
                calibIds.add(charucoIds)
                message.postValue("Calibrando camera %d/%d".format(calibIds.size, minCalibFrames))
                if (calibIds.size >= minCalibFrames) {
                    Log.i(TAG, calibCorners.toString())
                    Log.i(TAG, calibIds.toString())
                    Aruco.calibrateCameraCharuco(calibCorners, calibIds, board, Size(mat.cols().toDouble(), mat.rows().toDouble()), cameraMatrix, distCoeffs)
                    storeCameraParameters()
                    isCalibrated = true
                }
            } else {
                message.postValue("Nenhuma placa de marcadores detectada ...")
            }
        } else {
            message.postValue("Nenhum marcador detectado, encontradas %d falsas detecções".format(rejected.size))
        }
    }

    private fun storeCameraParameters() {
        val cameraData = DoubleArray((cameraMatrix!!.total()*cameraMatrix!!.channels()).toInt())
        val distData = DoubleArray((distCoeffs!!.total()*distCoeffs!!.channels()).toInt())
        Log.i(TAG, cameraMatrix!!.toString())
        Log.i(TAG, distCoeffs!!.toString())
        cameraMatrix!!.get(0, 0, cameraData)
        distCoeffs!!.get(0, 0, distData)
        val cameraParameters = json {
            obj(
                "cameraMatrix" to obj(
                    "rows" to cameraMatrix!!.rows(),
                    "cols" to cameraMatrix!!.cols(),
                    "format" to cameraMatrix!!.type(),
                    "data" to array(cameraData.asList())
                ),
                "distCoeffs" to obj(
                    "rows" to distCoeffs!!.rows(),
                    "cols" to distCoeffs!!.cols(),
                    "format" to distCoeffs!!.type(),
                    "data" to array(distData.asList())
                )
            )
        }
        Log.i(TAG, cameraParameters.toJsonString())
        val file = File(fileDir, "cameraParameters.json")
        file.writeText(cameraParameters.toJsonString())
    }
}
