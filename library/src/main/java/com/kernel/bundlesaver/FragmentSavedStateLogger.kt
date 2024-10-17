package com.kernel.bundlesaver

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * [FragmentManager.FragmentLifecycleCallbacks] implementation that logs information about the
 * saved state of Fragments.
 */
internal class FragmentSavedStateLogger(
    private val formatter: Formatter,
    private val logger: Logger
) : FragmentManager.FragmentLifecycleCallbacks() {

    private val savedStates = HashMap<Fragment, Bundle>()
    private var isLogging = true

    override fun onFragmentSaveInstanceState(fragmentManager: FragmentManager, fragment: Fragment, outState: Bundle) {
        if (isLogging) {
            savedStates[fragment] = outState
        }
    }

    override fun onFragmentStopped(fragmentManager: FragmentManager, fragment: Fragment) {
        logAndRemoveSavedState(fragment, fragmentManager)
    }

    override fun onFragmentDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
        logAndRemoveSavedState(fragment, fragmentManager)
    }

    private fun logAndRemoveSavedState(fragment: Fragment, fragmentManager: FragmentManager) {
        val savedState = savedStates.remove(fragment)
        if (savedState != null) {
            try {
                val message = formatter.format(
                    fragmentManager = fragmentManager,
                    fragment = fragment,
                    bundle = savedState
                )
                logger.log(message = message)
            } catch (exception: RuntimeException) {
                logger.logException(exception = exception)
            }
        }
    }

    internal fun startLogging() {
        isLogging = true
    }

    internal fun stopLogging() {
        isLogging = false
    }
}