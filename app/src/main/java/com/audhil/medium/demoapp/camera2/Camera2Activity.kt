package com.audhil.medium.demoapp.camera2

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import com.audhil.medium.demoapp.R
import kotlinx.android.synthetic.main.camera2_activity.*
import java.util.*
import android.support.v4.app.ActivityCompat
import android.view.TextureView
import com.audhil.medium.demoapp.util.showToast
import com.audhil.medium.demoapp.util.showVLog
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.Scalar
import org.opencv.features2d.FeatureDetector
import org.opencv.features2d.Features2d
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.nio.ByteBuffer

/*
 * Created by mohammed-2284 on 31/03/18.
 */


class Camera2Activity : AppCompatActivity() {

    private var cameraManager: CameraManager? = null
    private var cameraFacing: Int = 0

    //  state callback
    private var cameraDeviceStateCallback: CameraDevice.StateCallback? = null
    //  camera device
    private var cameraDevice: CameraDevice? = null

    private var surfaceTextureListener: TextureView.SurfaceTextureListener? = null

    private var mImageReader: ImageReader? = null
    private var onImageAvailableListener: ImageReader.OnImageAvailableListener? = null

    private var cameraId: String? = null
    private var previewSize: Size? = null

    //  bg thread and handler
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var captureRequest: CaptureRequest? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    //  feature detector
    var detector: FeatureDetector? = null
    private var THRESHOLD = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera2_activity)

        //  camera manager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        //  back camera
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK

        //  texture listener
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture?): Boolean = false

            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
                setUpCamera()
                openCamera()
            }
        }

        //  state callback
        cameraDeviceStateCallback = object : CameraDevice.StateCallback() {

            override fun onOpened(cameraDevice: CameraDevice) {
                this@Camera2Activity.cameraDevice = cameraDevice
                createPreviewSession()
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                cameraDevice.close()
                this@Camera2Activity.cameraDevice = null
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                cameraDevice.close()
                this@Camera2Activity.cameraDevice = null
            }
        }

        //  Image Available Listener
        onImageAvailableListener = ImageReader.OnImageAvailableListener {
            val image = it.acquireLatestImage() ?: return@OnImageAvailableListener
            backgroundHandler?.post(ImageManipulation(image, detector))
        }
    }

    //  loader call back
    private val mLoaderCallback = object : BaseLoaderCallback(this) {

        override fun onManagerConnected(status: Int) = when (status) {

            LoaderCallbackInterface.SUCCESS -> {
                showVLog("OpenCV loaded successfully")
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
        //  ORB feature detector
        detector = FeatureDetector.create(FeatureDetector.ORB)
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

        openBackgroundThread()
        if (texture_view.isAvailable) {
            setUpCamera()
            openCamera()
        } else texture_view.surfaceTextureListener = surfaceTextureListener
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        closeBackgroundThread()
    }

    //  close background thread
    private fun closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread?.quitSafely()
            backgroundThread = null
            backgroundHandler = null
        }
    }

    //  close camera
    private fun closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession!!.close()
            cameraCaptureSession = null
        }
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    //  setup camera
    private fun setUpCamera() =
            try {
                //  looping through available cameras list
                cameraManager?.cameraIdList?.forEach {
                    val cameraCharacteristics = cameraManager?.getCameraCharacteristics(it)
                    if (cameraCharacteristics?.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                        val streamConfigMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        //  The zeroth element is the resolution we want — the highest available one
                        previewSize = streamConfigMap.getOutputSizes(SurfaceTexture::class.java)[0]
                        mImageReader = ImageReader.newInstance(previewSize!!.width / 16, previewSize!!.height / 16, ImageFormat.YUV_420_888, 2)
                        mImageReader?.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                        cameraId = it
                    }
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

    //  open camera
    private fun openCamera() =
            try {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    cameraManager?.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler)
                else Unit
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

    //  back ground thread
    private fun openBackgroundThread() {
        backgroundThread = HandlerThread("back_ground_thread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    //  camera preview session
    private fun createPreviewSession() =
            try {
                //  surface texture
                val surfaceTexture = texture_view.surfaceTexture
                surfaceTexture.setDefaultBufferSize(previewSize?.width!!, previewSize?.height!!)

                //  preview surface
                val previewSurface = Surface(surfaceTexture)
                val mImageSurface = mImageReader?.surface

                captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                captureRequestBuilder?.addTarget(mImageSurface)
                captureRequestBuilder?.addTarget(previewSurface)

                cameraDevice?.createCaptureSession(Arrays.asList(mImageSurface, previewSurface),
                        object : CameraCaptureSession.StateCallback() {

                            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                if (cameraDevice == null)
                                    return

                                try {
                                    captureRequest = captureRequestBuilder?.build()
                                    this@Camera2Activity.cameraCaptureSession = cameraCaptureSession
                                    this@Camera2Activity.cameraCaptureSession?.setRepeatingRequest(captureRequest, null, backgroundHandler)
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            }
                        }, backgroundHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

    //  ImageManipulation class
    inner class ImageManipulation(private val acquireLatestImage: Image, val detector: FeatureDetector?) : Runnable {

        private lateinit var keyPoints: MatOfKeyPoint
        private var RED_COLOR = Scalar(255.0, 0.0, 0.0)

        override fun run() {
            showVLog("acquireLatestImage.height : " + acquireLatestImage.height)
            showVLog("acquireLatestImage.width : " + acquireLatestImage.width)
            showVLog("acquireLatestImage.format : " + acquireLatestImage.format)
            try {
                //  making mat of preview image
                val inputMat = imageToMat(acquireLatestImage)
                showVLog("acquireLatestImage : Resultant Mat of an Image is : $inputMat")

                //  making feature detection with OpenCV ORB detector
                keyPoints = MatOfKeyPoint()
                detector?.detect(inputMat, keyPoints, Mat())
                val outputImage = Mat()

                //  drawing all keypoints without THRESHOLD
//                println("acquireLatestImage : threshold : keyPoints?.size() : " + keyPoints.size())
//                Features2d.drawKeypoints(inputMat, keyPoints, outputImage, RED_COLOR, Features2d.DRAW_RICH_KEYPOINTS);
//                Imgproc.resize(outputImage, outputImage, inputMat.size())
//
//                val bitmap = Bitmap.createBitmap(outputImage.cols(), outputImage.rows(), Bitmap.Config.ARGB_8888)
//                Utils.matToBitmap(outputImage, bitmap)
//
//                //  plotting in Android View
//                runOnUiThread {
//                    bitmap?.let {
//                        println("---Resultant bitmap : " + bitmap)
//                        image_view.setImageBitmap(bitmap)
//                    } ?: "Nothing can be done! Sorry!".showToast()
//                }


                //  drawing key point with THRESHOLD
                val listOfKeyPoints = keyPoints.toList()
                println("size : keyPoints?.size() : " + keyPoints.size())
                println("size : listOfKeyPoints?.size : " + listOfKeyPoints?.size)
                listOfKeyPoints?.sortWith(Comparator { kp1, kp2 ->
                    // Sort them in descending order, so the best response KPs will come first
                    (kp2.response - kp1.response).toInt()
                })

                listOfKeyPoints?.size?.let {
                    if (it >= THRESHOLD) {
                        //  picking first 300 points
                        val listOfBestKeyPoints = listOfKeyPoints.subList(0, THRESHOLD)
                        println("size : listOfBestKeyPoints?.size : " + listOfBestKeyPoints.size)
                        //  converting list to key point
                        val finalKeyPoints = MatOfKeyPoint()
                        finalKeyPoints.fromList(listOfBestKeyPoints)
                        //  draw keypoints in outputImage
                        Features2d.drawKeypoints(inputMat, finalKeyPoints, outputImage, RED_COLOR, Features2d.DRAW_RICH_KEYPOINTS);
                        //  resize it
                        Imgproc.resize(outputImage, outputImage, inputMat.size())
                    } else {
                        val finalKeyPoints = MatOfKeyPoint()
                        finalKeyPoints.fromList(listOfKeyPoints)
                        //  draw keypoints in outputImage
                        Features2d.drawKeypoints(inputMat, finalKeyPoints, outputImage, RED_COLOR, Features2d.DRAW_RICH_KEYPOINTS);
                        //  resize it
                        Imgproc.resize(outputImage, outputImage, inputMat.size())
                    }
                }

                val bitmap = Bitmap.createBitmap(outputImage.cols(), outputImage.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(outputImage, bitmap)

                //  plotting in Android View
                runOnUiThread {
                    bitmap?.let {
                        println("---Resultant bitmap : " + bitmap)
                        image_view.setImageBitmap(bitmap)
                    } ?: "Nothing can be done! Sorry!".showToast()
                }


            } catch (e: Exception) {
                "Something went wrong".showToast()
                e.printStackTrace()
            } finally {
                acquireLatestImage.close()
            }
        }

        //  image to mat converter
        private fun imageToMat(image: Image): Mat {
            var buffer: ByteBuffer
            var rowStride: Int
            var pixelStride: Int
            val width = image.width
            val height = image.height
            var offset = 0

            val planes = image.planes
            val data = ByteArray(image.width * image.height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
            val rowData = ByteArray(planes[0].rowStride)

            for (i in planes.indices) {
                buffer = planes[i].buffer
                rowStride = planes[i].rowStride
                pixelStride = planes[i].pixelStride
                val w = if (i == 0) width else width / 2
                val h = if (i == 0) height else height / 2
                for (row in 0 until h) {
                    val bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
                    if (pixelStride == bytesPerPixel) {
                        val length = w * bytesPerPixel
                        buffer.get(data, offset, length)

                        // Advance buffer the remainder of the row stride, unless on the last row.
                        // Otherwise, this will throw an IllegalArgumentException because the buffer
                        // doesn't include the last padding.
                        if (h - row != 1) {
                            buffer.position(buffer.position() + rowStride - length)
                        }
                        offset += length
                    } else {

                        // On the last row only read the width of the image minus the pixel stride
                        // plus one. Otherwise, this will throw a BufferUnderflowException because the
                        // buffer doesn't include the last padding.
                        if (h - row == 1) {
                            buffer.get(rowData, 0, width - pixelStride + 1)
                        } else {
                            buffer.get(rowData, 0, rowStride)
                        }

                        for (col in 0 until w) {
                            data[offset++] = rowData[col * pixelStride]
                        }
                    }
                }
            }

            // Finally, create the Mat.
            val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
            mat.put(0, 0, data)

            return mat
        }
    }
}