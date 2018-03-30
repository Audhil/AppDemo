package com.audhil.medium.demoapp

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.SurfaceView
import android.view.WindowManager
import com.audhil.medium.demoapp.util.showVLog
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.Scalar
import org.opencv.features2d.FeatureDetector
import org.opencv.features2d.Features2d
import org.opencv.imgproc.Imgproc
import java.io.IOException

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var RED_COLOR = Scalar(255.0, 0.0, 0.0)

    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var detector: FeatureDetector? = null
    private var keyPoints: MatOfKeyPoint? = null

    //  loader call back
    private val mLoaderCallback = object : BaseLoaderCallback(this) {

        override fun onManagerConnected(status: Int) = when (status) {

            LoaderCallbackInterface.SUCCESS -> {
                showVLog("OpenCV loaded successfully")
                mOpenCvCameraView?.enableView()
                try {
                    initializeOpenCVDependencies()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            else ->
                super.onManagerConnected(status)
        }
    }

    @Throws(IOException::class)
    private fun initializeOpenCVDependencies() {
        mOpenCvCameraView?.enableView()

        //  ORB feature detector
        detector = FeatureDetector.create(FeatureDetector.ORB)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.main_activity)

        //  checking camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.java_camera_view)
        mOpenCvCameraView?.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView?.setCvCameraViewListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            showVLog("Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
        } else {
            showVLog("OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView?.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat = recognize(inputFrame.rgba())


    //  recognize from camera output
    private fun recognize(aInputFrame: Mat): Mat {
        Imgproc.cvtColor(aInputFrame, aInputFrame, Imgproc.COLOR_RGB2GRAY)
        keyPoints = MatOfKeyPoint()
        detector?.detect(aInputFrame, keyPoints, Mat())

        //  drawing key point
        val outputImage = Mat()
        val flags = Features2d.DRAW_RICH_KEYPOINTS
        Features2d.drawKeypoints(aInputFrame, keyPoints, outputImage, RED_COLOR, flags);
        Imgproc.resize(outputImage, outputImage, aInputFrame.size())
        return outputImage
    }
}