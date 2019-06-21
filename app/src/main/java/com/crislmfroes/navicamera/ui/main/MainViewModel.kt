package com.crislmfroes.navicamera.ui.main

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import com.beust.klaxon.*
import com.crislmfroes.navicamera.model.*
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.aruco.Aruco
import org.opencv.aruco.CharucoBoard
import org.opencv.aruco.DetectorParameters
import org.opencv.aruco.Dictionary
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.sinh

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    private var markerDict : Dictionary? = null
    private var board : CharucoBoard? = null
    private val calibCorners = mutableListOf<Mat>()
    private val calibIds = mutableListOf<Mat>()
    private val minCalibFrames = 25
    private val fileDir = application.filesDir
    private var cameraMatrix : Mat? = null
    private var distCoeffs : Mat? = null
    private var isCalibrated = false
    private val detectorParameters : DetectorParameters
    val message = MutableLiveData<String>()
    private var isProcessing = false
    //val overlay : MutableLiveData<Bitmap> = MutableLiveData()
    private val marcadorRepository : MarcadorRepository
    val marcadores = MutableLiveData<List<Marcador>>()
    private var allMarcadores : List<Marcador>? = listOf()
    val loading = MutableLiveData<Boolean>(false)
    val loadingMsg = MutableLiveData<String>()
    val loadingProgress = MutableLiveData<Int>()

    init {
        val database = MarcadorDatabaseRoom.getDatabase(application, viewModelScope)
        val marcadorDao = database.marcadorDao()
        val dicionarioDao = database.dicionarioDao()
        val marcadorRemote = MarcadorRemote()
        marcadorRepository = MarcadorRepository(marcadorDao, marcadorRemote, dicionarioDao)
        detectorParameters = DetectorParameters.create()
        //allMarcadores = marcadorRepository.getAllMarcador()
        Log.i(TAG, "Populando banco de dados ...")
        populateRoomDatabase()
        Log.i(TAG, "Carregando dicionário de marcadores ...")
        loadDictionary("dictByteList.json")
        Log.i(TAG, "Carregando parâmetros da camera ...")
        loadCameraParameters()
    }

    private fun populateRoomDatabase() = viewModelScope.launch {
        /*val allRemote = marcadorRepository.getAllMarcadorRemote().value
        Log.i(TAG, "All remote: %s".format(allRemote.toString()))
        allRemote?.forEach {
            marcadorRepository.insertMarcador(it)
        }
        allMarcadores = allRemote*/
        val tmpMarcadores = marcadorRepository.getAllMarcador()
        if (tmpMarcadores.isNullOrEmpty()) {
            val allRemote = marcadorRepository.getAllMarcadorRemote().value
            Log.i(TAG, "All remote: %s".format(allRemote.toString()))
            allRemote?.forEach {
                marcadorRepository.insertMarcador(it)
            }
            allMarcadores = allRemote
        } else {
            allMarcadores = tmpMarcadores
        }
    }

    private fun loadDictionary(filename : String) = viewModelScope.launch {
        val first = marcadorRepository.getFirstDicionario()
        if (first != null) {
            markerDict = first.cvDictionary
        } else {
            Log.i(TAG, "Carregando dicionário da pasta assets ...")
            val stream = getApplication<Application>().assets.open(filename)
            val parser = Parser.default()
            val json = parser.parse(stream) as JsonObject
            stream.close()
            Log.i(TAG, "Terminou de carregar o dicionário ...")
            val maxCorrectionBits = json["maxCorrectionBits"] as Int
            val data = (json["bytesList"] as JsonObject)["data"] as JsonArray<Byte>
            val markerSize = json["markerSize"] as Int
            val dicionario = Dicionario(markerSize = markerSize, maxCorretionBits = maxCorrectionBits, bytesList = data.toByteArray(), nMarkers = 1000)
            Log.i(TAG, "Inserindo dicionário no banco ...")
            marcadorRepository.insertDicionario(dicionario)
            Log.i(TAG, "Dicionário inserido com sucesso ...")
            markerDict = dicionario.cvDictionary
        }
        board = CharucoBoard.create(10, 10, 0.02f, 0.016f, markerDict)
    }

    private fun loadCameraParameters() = viewModelScope.launch {
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
                loading.postValue(isCalibrated)
            } catch (e : FileNotFoundException) {
                isCalibrated = false
                loading.postValue(isCalibrated)
                loadingMsg.postValue("Calibrando 0/%d".format(minCalibFrames))
                loadingProgress.postValue(0)
                cameraMatrix = Mat()
                distCoeffs = Mat()
                //message.postValue("Calibrando camera 0/%d".format(minCalibFrames))
            }
        }
    }

    fun processImage(mat : Mat) : Mat {
        //Log.i(TAG, "Board: %s, Dict: %s".format(board.toString(), markerDict.toString()))
        //Log.i(TAG, "Marcadores: %s".format(allMarcadores.toString()))
        val rgb = Mat(mat.rows(), mat.cols(), CvType.CV_8UC3)
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_RGBA2BGR)
        if (board != null && markerDict != null) {
            /*val mat = Mat(frame.size.width, frame.size.height, CvType.CV_8UC3)
            val yuv = Mat(frame.size.height+frame.size.height/2, frame.size.width, CvType.CV_8UC1)
            yuv.put(0, 0, frame.data)
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2BGR_NV21)
            Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE)*/
            val marcadorList = mutableListOf<Marcador>()
            isProcessing = true
            if (!isCalibrated) {
                calibrateCamera(rgb)
            } else {
                val corners = mutableListOf<Mat>()
                val ids = Mat()
                val rejected = mutableListOf<Mat>()
                Aruco.detectMarkers(rgb, markerDict, corners, ids, detectorParameters, rejected)
                Aruco.drawDetectedMarkers(rgb, corners, ids, Scalar(0.0, 0.0, 255.0))
                //message.postValue("Detectados %d marcadores".format(corners.size))
                if (corners.size > 0) {
                    val rvecs = Mat()
                    val tvecs = Mat()
                    Aruco.estimatePoseSingleMarkers(corners, 0.2f, cameraMatrix, distCoeffs, rvecs, tvecs)
                    for (i in 0 until ids.total()) {
                        val rmat = Mat(3, 1, CvType.CV_32FC1)
                        val rdata = DoubleArray(3)
                        rvecs.get(i.toInt(), 0, rdata)
                        rmat.put(0, 0, *rdata)
                        val tmat = Mat(3, 1, CvType.CV_32FC1)
                        val tdata = DoubleArray(3)
                        tvecs.get(i.toInt(), 0, tdata)
                        tmat.put(0, 0, *tdata)
                        Calib3d.drawFrameAxes(rgb, cameraMatrix, distCoeffs, rmat, tmat, 0.2f)
                        val id = ids[i.toInt(), 0][0].toInt()
                        allMarcadores?.let {
                            for (marcador in it) {
                                if (marcador.cod == id + 1) {
                                    val distancia = Core.norm(tmat)
                                    val x = tdata[0]
                                    val y = tdata[1]
                                    //val distancia = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0))
                                    val angle = (sinh(x/distancia))*180/Math.PI
                                    marcador.rotacao = angle.toFloat()
                                    marcador.distancia = distancia.toFloat()
                                    marcadorList.add(marcador)
                                    break
                                }
                            }
                        }
                        rmat.release()
                        tmat.release()
                    }
                    rvecs.release()
                    tvecs.release()
                }
                ids.release()
            }
            isProcessing = false
            /*val bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bitmap)
            overlay.postValue(bitmap)*/
            marcadorList.sortBy {
                it.distancia
            }
            marcadores.postValue(marcadorList)
        }
        Imgproc.cvtColor(rgb, mat, Imgproc.COLOR_BGR2RGBA)
        return mat
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
                //message.postValue("Calibrando camera %d/%d".format(calibIds.size, minCalibFrames))
                loadingProgress.postValue((calibIds.size/minCalibFrames.toFloat()*100).toInt())
                loadingMsg.postValue("Calibrando %d/%d".format(calibIds.size, minCalibFrames))
                if (calibIds.size >= minCalibFrames) {
                    Log.i(TAG, calibCorners.toString())
                    Log.i(TAG, calibIds.toString())
                    Aruco.calibrateCameraCharuco(calibCorners, calibIds, board, Size(mat.cols().toDouble(), mat.rows().toDouble()), cameraMatrix, distCoeffs)
                    storeCameraParameters()
                    isCalibrated = true
                    loading.postValue(isCalibrated)
                }
            } else {
                message.postValue("Nenhuma placa de marcadores detectada ...")
            }
        } else {
            message.postValue("Nenhum marcador detectado")
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
