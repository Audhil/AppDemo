package com.audhil.medium.demoapp.util

import android.util.Log

/**
 *
 * Created by mohammed-2284 on 27/03/18.
 */

object LogUtil {

    val DEBUG_BOOL = true

    fun v(tag: String, msg: String) {
        if (DEBUG_BOOL) {
            Log.v(tag, msg)
        }
    }

    fun e(tag: String, msg: String) {
        if (DEBUG_BOOL) {
            Log.e(tag, msg)
        }
    }

    fun d(tag: String, msg: String) {
        if (DEBUG_BOOL) {
            Log.d(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (DEBUG_BOOL) {
            Log.i(tag, msg)
        }
    }

    fun w(tag: String, msg: String) {
        if (DEBUG_BOOL) {
            Log.w(tag, msg)
        }
    }
}