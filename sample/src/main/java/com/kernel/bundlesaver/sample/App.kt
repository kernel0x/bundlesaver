package com.kernel.bundlesaver.sample

import android.app.Application
import com.kernel.bundlesaver.BundleManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        BundleManager.initialize(this)
    }
}