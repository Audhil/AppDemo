package com.audhil.medium.demoapp

import android.app.Application

/*
 * Created by mohammed-2284 on 31/03/18.
 */

class AppApplication : Application() {

    companion object {
        lateinit var INSTANCE: AppApplication
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}