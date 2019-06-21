package com.crislmfroes.navicamera.ui.main

import android.graphics.*
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crislmfroes.navicamera.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.cameraview.Audio
import com.otaliastudios.cameraview.CameraView
import kotlinx.android.synthetic.main.main_fragment.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class MainFragment : Fragment(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var cameraView : JavaCameraView
    //private lateinit var overlay : ImageView

    private val TAG = "MainFragment"

    private var showPreview = true

    private var mRgba : Mat? = null
    private lateinit var mRgbaF : Mat
    private lateinit var mRgbaT : Mat

    private var lastMsg : String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCv", "Unable to load OpenCV")
        } else {
            Log.d("OpenCv", "OpenCV loaded")
        }
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.message.observe(this, Observer {
            //textView.text = it
            if (it != lastMsg) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                lastMsg = it
            }
        })
        viewModel.marcadores.observe(this, Observer {
            val adapter = MarcadorAdapter(it)
            recyclerview.adapter = adapter
        })
        viewModel.loading.observe(this, Observer {
            if (it) {
                progressbar.visibility = View.GONE
                progresslabel.visibility = View.GONE
            } else {
                progressbar.visibility = View.VISIBLE
                progresslabel.visibility = View.VISIBLE
            }
        })
        viewModel.loadingMsg.observe(this, Observer {
            progresslabel.text = it
        })
        viewModel.loadingProgress.observe(this, Observer {
            progressbar.progress = it
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraView = view.findViewById(R.id.cameraview)
        //overlay = view.findViewById(R.id.overlay)
        /*cameraView.audio = Audio.OFF
        cameraView.setLifecycleOwner(this)
        cameraView.addFrameProcessor {
            viewModel.processImage(it)
        }*/
        val fab = view.findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            showPreview = !showPreview
            if (showPreview) {
                //overlay.visibility = View.VISIBLE
                //cameraView.visibility = View.VISIBLE
                recyclerview.visibility = View.GONE
            } else {
                //overlay.visibility = View.GONE
                //cameraView.visibility = View.INVISIBLE
                recyclerview.visibility = View.VISIBLE
            }
        }
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerview)
        val layoutManager = LinearLayoutManager(this.context)
        recycler.layoutManager = layoutManager
        cameraView.setCvCameraViewListener(this)
        cameraView.enableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mRgbaF = Mat(height, width, CvType.CV_8UC4)
        mRgbaT = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mRgba?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = inputFrame?.rgba()
        Core.transpose(mRgba, mRgbaT)
        //Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size())
        Core.flip(mRgbaT, mRgbaT, 1)
        //Core.flip(mRgbaF, mRgba, 1)
        mRgbaT = viewModel.processImage(mRgbaT)
        Imgproc.resize(mRgbaT, mRgba, mRgba!!.size())
        return mRgba!!
    }

}
