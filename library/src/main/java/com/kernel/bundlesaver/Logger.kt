package com.kernel.bundlesaver

import android.util.Log

/**
 * Interface that provides flexibility in how BundleSaver's output is logged.
 * The default implementation [DefaultLogger] should be suitable for most use cases.
 */
interface Logger {
    fun log(message: String)
    fun logException(exception: Exception)
}

/**
 * The default implementation of the [Logger] interface, which logs messages to Logcat.
 */
class DefaultLogger(private val priority: Int = Log.DEBUG, private val tag: String = "BundleSaver") : Logger {

    override fun log(message: String) {
        Log.println(priority, tag, message)
    }

    override fun logException(exception: Exception) {
        Log.w(tag, exception.message, exception)
    }
}