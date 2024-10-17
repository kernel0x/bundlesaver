package com.kernel.bundlesaver.wrapper

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.nio.ByteBuffer

/**
 * A wrapper class for a [Bitmap] that allows it to be placed into a [android.os.Bundle]
 * and written to disk.
 */
internal class BitmapWrapper(internal val bitmap: Bitmap) : Parcelable {

    //region Parcelable
    constructor(parcel: Parcel) : this(
        bitmap = Bitmap.createBitmap(
            parcel.readInt(),
            parcel.readInt(),
            Bitmap.Config.values()[parcel.readInt()]
        ).apply {
            parcel.createByteArray()?.also {
                copyPixelsFromBuffer(ByteBuffer.wrap(it))
            }
        }
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val size = bitmap.allocationByteCount
        val byteBuffer = ByteBuffer.allocate(size)
        bitmap.copyPixelsToBuffer(byteBuffer)

        with(dest) {
            writeInt(bitmap.width)
            writeInt(bitmap.height)
            writeInt(bitmap.config.ordinal)
            writeByteArray(byteBuffer.array())
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<BitmapWrapper> {
        override fun createFromParcel(parcel: Parcel): BitmapWrapper = BitmapWrapper(parcel)

        override fun newArray(size: Int): Array<BitmapWrapper?> = arrayOfNulls(size)
    }
    //endregion Parcelable
}

/**
 * Manages the wrapping and unwrapping of certain [android.os.Parcelable] objects that use
 * native code to optimize their [android.os.Parcelable] implementations. If these objects are placed into a [Bundle]
 * unwrapped, they may cause a crash if the [Bundle] is written to a [android.os.Parcel] that then calls
 * [Parcel.marshall].
 */
internal fun unwrapOptimizedObjects(bundle: Bundle) {
    val keys = bundle.keySet()
    for (key in keys) {
        val value = bundle.get(key)
        if (value is BitmapWrapper) {
            bundle.putParcelable(key, value.bitmap)
        }
    }
}

internal fun wrapOptimizedObjects(bundle: Bundle) {
    val keys = bundle.keySet()
    for (key in keys) {
        val value = bundle.get(key)
        if (value is Bitmap) {
            bundle.putParcelable(key, BitmapWrapper(value))
        }
    }
}