package com.kernel.bundlesaver

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.kernel.bundlesaver.utils.bundleBreakdown
import com.kernel.bundlesaver.utils.kb

/**
 * Interface that provides flexibility in how BundleSaver's output is formatted.
 * The default implementation [DefaultFormatter] should be suitable for most use cases.
 */
interface Formatter {
    fun format(activity: Activity, bundle: Bundle): String
    fun format(fragmentManager: FragmentManager, fragment: Fragment, bundle: Bundle): String
    fun format(size: Int, limit: Int): String
}

/**
 * The default implementation of the [Formatter] interface.
 */
class DefaultFormatter : Formatter {

    override fun format(activity: Activity, bundle: Bundle): String {
        return "${activity.javaClass.simpleName}.onSaveInstanceState wrote: ${bundleBreakdown(bundle)}"
    }

    override fun format(fragmentManager: FragmentManager, fragment: Fragment, bundle: Bundle): String {
        var message = "${fragment.javaClass.simpleName}.onSaveInstanceState wrote: ${bundleBreakdown(bundle)}"
        val fragmentArguments = fragment.arguments
        if (fragmentArguments != null) {
            message += "\n* fragment arguments = ${bundleBreakdown(fragmentArguments)}"
        }
        return message
    }

    override fun format(size: Int, limit: Int): String {
        return "Bundle size exceeded the limit of ${kb(limit)} KB. Current size: ${kb(size)}"
    }
}