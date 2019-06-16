package com.crislmfroes.navicamera

import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.aruco.Aruco
import org.opencv.aruco.DetectorParameters
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

@RunWith(AndroidJUnit4::class)
class OpenCvTest {
    @Test
    fun detectionWorks() {
        val appContext = InstrumentationRegistry.getTargetContext()
        OpenCVLoader.initDebug()
        val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_ARUCO_ORIGINAL)
        val img = Mat(600, 600, CvType.CV_8UC3)
        Aruco.drawMarker(dictionary, 0, 600, img)
        val corners = mutableListOf<Mat>()
        val ids = Mat()
        val config = DetectorParameters.create()
        config._adaptiveThreshWinSizeMax = 700
        config._minDistanceToBorder = 0
        Log.i("Tests", img.type().toString())
        Aruco.detectMarkers(img, dictionary, corners, ids, config)
        Assert.assertNotEquals(corners.size, 0)
    }
}