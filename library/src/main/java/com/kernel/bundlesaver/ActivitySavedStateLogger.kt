package com.kernel.bundlesaver

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.kernel.bundlesaver.exceptions.BundleSizeExceededException
import com.kernel.bundlesaver.utils.sizeAsParcel

/**
 * [Application.ActivityLifecycleCallbacks] implementation that logs information
 * about the saved state of Activities.
 */
internal class ActivitySavedStateLogger(
    private val formatter: Formatter,
    private val logger: Logger,
    private val limitSizeBundle: Int,
    logFragments: Boolean
) : ActivityLifecycleCallbacksAdapter() {

    private val fragmentLogger = if (logFragments) FragmentSavedStateLogger(formatter, logger) else null
    private val savedStates = HashMap<Activity, Bundle>()
    internal var isLogging = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity && fragmentLogger != null) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLogger, true)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        logAndRemoveSavedState(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (isLogging) {
            savedStates[activity] = outState
        }
    }

    override fun onActivityStopped(activity: Activity) {
        logAndRemoveSavedState(activity)
    }

    private fun logAndRemoveSavedState(activity: Activity) {
        val savedState = savedStates.remove(key = activity)
        if (savedState != null) {
            try {
                val message = formatter.format(activity = activity, bundle = savedState)
                logger.log(message = message)
                val size = sizeAsParcel(bundle = savedState)
                if (size > limitSizeBundle) {
                    logger.logException(
                        exception = BundleSizeExceededException(
                            formatter.format(
                                size = size,
                                limit = limitSizeBundle
                            )
                        )
                    )
                }
            } catch (exception: RuntimeException) {
                logger.logException(exception = exception)
            }
        }
    }

    fun startLogging() {
        isLogging = true
        fragmentLogger?.startLogging()
    }

    fun stopLogging() {
        isLogging = false
        savedStates.clear()
        fragmentLogger?.stopLogging()
    }
}