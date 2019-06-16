package com.crislmfroes.navicamera.ui.main

import android.graphics.*
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crislmfroes.navicamera.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.cameraview.Audio
import com.otaliastudios.cameraview.CameraView
import kotlinx.android.synthetic.main.main_fragment.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var cameraView : CameraView
    private lateinit var overlay : ImageView

    private val TAG = "MainFragment"

    private var showPreview = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.message.observe(this, Observer {
            textView.text = it
        })
        viewModel.overlay.observe(this, Observer {
            val matrix = Matrix()
            matrix.setRotate(180.0f)
            val bitmap = it
            overlay.setImageBitmap(bitmap)
        })
        viewModel.marcadores.observe(this, Observer {
            val adapter = MarcadorAdapter(it)
            recyclerview.adapter = adapter
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraView = view.findViewById(R.id.cameraview)
        overlay = view.findViewById(R.id.overlay)
        cameraView.audio = Audio.OFF
        cameraView.setLifecycleOwner(this)
        cameraView.addFrameProcessor {
            it.freeze()
            val mat = Mat(it.size.width, it.size.height, CvType.CV_8UC3)
            val yuv = Mat(it.size.height+it.size.height/2, it.size.width, CvType.CV_8UC1)
            yuv.put(0, 0, it.data)
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2BGR_NV21)
            viewModel.processImage(mat)
            it.release()
        }
        val fab = view.findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            showPreview = !showPreview
            if (showPreview) {
                overlay.visibility = View.VISIBLE
                recyclerview.visibility = View.GONE
            } else {
                overlay.visibility = View.GONE
                recyclerview.visibility = View.VISIBLE
            }
        }
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerview)
        val layoutManager = LinearLayoutManager(this.context)
        recycler.layoutManager = layoutManager
    }

}
