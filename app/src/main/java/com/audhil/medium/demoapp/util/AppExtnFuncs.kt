package com.audhil.medium.demoapp.util

import android.widget.Toast
import com.audhil.medium.demoapp.AppApplication

/**
 *
 * Created by mohammed-2284 on 27/03/18.
 */

//  show logs
fun Any.showVLog(log: String) = LogUtil.v(this::class.java.simpleName, log)

fun Any.showELog(log: String) = LogUtil.e(this::class.java.simpleName, log)

fun Any.showDLog(log: String) = LogUtil.d(this::class.java.simpleName, log)

fun Any.showILog(log: String) = LogUtil.i(this::class.java.simpleName, log)

fun Any.showWLog(log: String) = LogUtil.w(this::class.java.simpleName, log)

fun String.showToast() = Toast.makeText(AppApplication.INSTANCE, this, Toast.LENGTH_SHORT).show()

//  combining 2 lets together
fun <T1, T2> Pair<T1?, T2?>.biLet(callback: (T1, T2) -> Unit) {
    if (this.first != null && this.second != null)
        callback(this.first!!, this.second!!)
}