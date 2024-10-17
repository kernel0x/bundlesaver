package com.kernel.bundlesaver.sample

import android.app.Activity
import android.os.Bundle
import com.kernel.bundlesaver.BundleManager

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        BundleManager.restoreInstanceState(this, savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val largeObject = ByteArray(10 * 1024 * 1024) // 10 МБ
        outState.putByteArray("test", largeObject)
        super.onSaveInstanceState(outState)
        BundleManager.saveInstanceState(this, outState)
    }
}