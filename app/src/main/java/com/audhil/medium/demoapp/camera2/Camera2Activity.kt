package com.audhil.medium.demoapp.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
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
import android.support.v4.content.ContextCompat
import android.view.TextureView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera2_activity)

        //  checking camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)

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
            backgroundHandler?.post(ImageManipulation(image))
        }
    }

    override fun onResume() {
        super.onResume()
        openBackgroundThread()
        if (texture_view.isAvailable) {
            setUpCamera()
            openCamera()
        } else
            texture_view.surfaceTextureListener = surfaceTextureListener
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
}