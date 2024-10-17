package com.kernel.bundlesaver.utils

import android.os.Bundle
import android.os.Parcel

/**
 * Helper class for converting [Bundle] instances to and from bytes.
 */
internal object Converter {

    /**
     * Converts the given [Bundle] to raw bytes.
     *
     * Note that if the [Bundle] contains some highly specialized classes
     * [android.os.IBinder], this process will fail.
     */
    fun toBytes(bundle: Bundle): ByteArray {
        val parcel = Parcel.obtain()
        parcel.writeBundle(bundle)
        val bytes = parcel.marshall()
        parcel.recycle()
        return bytes
    }

    /**
     * Converts the given bytes to a [Bundle].
     *
     * Note that if the bytes do not represent a [Bundle] that was previously converted with
     * [toBytes], this process will likely fail and will return `null`.
     */
    fun fromBytes(bytes: ByteArray): Bundle? {
        val parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        val bundle: Bundle? = try {
            parcel.readBundle(Converter::class.java.classLoader)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
        parcel.recycle()
        return bundle
    }
}