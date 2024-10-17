package com.kernel.bundlesaver.utils

import android.os.Bundle
import android.os.Parcel
import com.kernel.bundlesaver.SizeTree
import java.util.Locale

/**
 * Measure the sizes of all the values in a typed [Bundle] when written to a
 * [Parcel]. Returns a map from keys to the sizes, in bytes, of the associated values in
 * the Bundle.
 */
internal fun sizeTreeFromBundle(bundle: Bundle): SizeTree {
    val results = ArrayList<SizeTree>(bundle.size())
    // We measure the totalSize of each value by measuring the total totalSize of the bundle before and
    // after removing that value and calculating the difference. We make a copy of the original
    // bundle so we can put all the original values back at the end. It's not possible to
    // carry out the measurements on the copy because of the way Android parcelables work
    // under the hood where certain objects are actually stored as references.
    val copy = Bundle(bundle)
    try {
        var bundleSize = sizeAsParcel(bundle)
        // Iterate over copy's keys because we're removing those of the original bundle
        for (key in copy.keySet()) {
            bundle.remove(key)
            val newBundleSize = sizeAsParcel(bundle)
            val valueSize = bundleSize - newBundleSize
            results.add(SizeTree(key, valueSize, emptyList()))
            bundleSize = newBundleSize
        }
    } finally {
        // Put everything back into original bundle
        bundle.putAll(copy)
    }
    return SizeTree("Bundle" + System.identityHashCode(bundle), sizeAsParcel(bundle), results)
}

/**
 * Measure the size of a typed [Bundle] when written to a [Parcel].
 */
internal fun sizeAsParcel(bundle: Bundle): Int {
    try {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(bundle)
            return parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    } catch (exception: OutOfMemoryError) {
        return -1
    }
}

/**
 * Converts the given number of bytes to kilobytes.
 */
internal fun kb(bytes: Int): Float {
    return bytes.toFloat() / 1000f
}

/**
 * Return a formatted String containing a breakdown of the contents of a [Bundle].
 *
 * @param bundle to format
 * @return a nicely formatted string (multi-line)
 */
fun bundleBreakdown(bundle: Bundle): String {
    val (key, totalSize, subTrees) = sizeTreeFromBundle(bundle)
    var result = String.format(
        Locale.UK,
        "%s contains %d keys and measures %,.1f KB when serialized as a Parcel",
        key,
        subTrees.size,
        kb(totalSize)
    )
    for ((key1, totalSize1) in subTrees) {
        result += String.format(
            Locale.UK,
            "\n* %s = %,.1f KB",
            key1, kb(totalSize1)
        )
    }
    return result
}