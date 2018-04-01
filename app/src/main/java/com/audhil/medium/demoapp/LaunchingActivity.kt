package com.audhil.medium.demoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.audhil.medium.demoapp.camera2.Camera2Activity
import com.audhil.medium.demoapp.util.showToast
import kotlinx.android.synthetic.main.launching_activity.*

/*
 * Created by mohammed-2284 on 01/04/18.
 */

class LaunchingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launching_activity)

        main_activity_btn.setOnClickListener {
            if (!checkCameraPermission()) {
                "Camera Permission needed to proceed further".showToast()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
                return@setOnClickListener
            }
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

        camera2_activity_btn.setOnClickListener {
            if (!checkCameraPermission()) {
                "Camera Permission needed to proceed further".showToast()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
                return@setOnClickListener
            }
            startActivity(Intent(applicationContext, Camera2Activity::class.java))
        }
    }

    //  check camera permission
    private fun checkCameraPermission(): Boolean =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}