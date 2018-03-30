package com.audhil.medium.demoapp.util

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