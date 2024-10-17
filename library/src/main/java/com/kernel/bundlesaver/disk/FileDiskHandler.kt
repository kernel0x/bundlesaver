package com.kernel.bundlesaver.disk

import android.content.Context
import androidx.annotation.Nullable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

private const val DIRECTORY_NAME = "bundle_saver"
private const val BACKGROUND_WAIT_TIMEOUT_MS = 1000L

/**
 * A simple implementation of [DiskHandler] that saves the requested data to individual files.
 * Similar to [android.content.SharedPreferences], this implementation will begin loading the
 * saved data into memory as soon as it is constructed and block on the first call until all data
 * is loaded (or some cutoff is reached).
 */
internal class FileDiskHandler(context: Context, executorService: ExecutorService) : DiskHandler {

    private val directory: File = context.getDir(DIRECTORY_NAME, Context.MODE_PRIVATE)
    private val pendingLoadFuture: Future<*> = executorService.submit { loadAllFiles() }
    private val keyByteMap = ConcurrentHashMap<String, ByteArray>()

    /**
     * Determines whether the [waitForFilesToLoad] call has completed in some way.
     */
    @Volatile
    private var isLoadedOrTimedOut = false

    override fun clearAll() {
        cancelFileLoading()
        keyByteMap.clear()
        deleteFilesByKey(null)
    }

    override fun clear(key: String) {
        cancelFileLoading()
        keyByteMap.remove(key)
        deleteFilesByKey(key)
    }

    override fun getBytes(key: String): ByteArray? {
        waitForFilesToLoad()
        return getBytesInternal(key)
    }

    override fun putBytes(key: String, bytes: ByteArray) {
        // Place the data in memory first
        keyByteMap[key] = bytes

        // Write the data to disk
        val file = File(directory, key)
        FileOutputStream(file).use { outStream ->
            outStream.write(bytes)
        }
    }

    /**
     * Cancels any pending file loading that happens at startup.
     */
    private fun cancelFileLoading() {
        pendingLoadFuture.cancel(true)
    }

    /**
     * Deletes all files associated with the given [key]. If the key is null, then
     * all stored files will be deleted.
     *
     * @param key The key associated with the data to delete (or null if all data should be
     *            deleted).
     */
    private fun deleteFilesByKey(key: String?) {
        directory.listFiles()?.forEach { file ->
            if (key == null || getFileNameForKey(key) == file.name) {
                file.delete()
            }
        }
    }

    @Nullable
    private fun getBytesFromDisk(key: String): ByteArray? {
        val file = getFileByKey(key) ?: return null

        return FileInputStream(file).use { inputStream ->
            val bytes = ByteArray(file.length().toInt())
            inputStream.read(bytes)
            bytes
        }
    }

    @Nullable
    private fun getBytesInternal(key: String): ByteArray? {
        // Check for data loaded into memory from the initial load
        val cachedBytes = keyByteMap[key]
        if (cachedBytes != null) {
            return cachedBytes
        }

        // Get bytes from disk and place into memory if necessary
        val bytes = getBytesFromDisk(key)
        if (bytes != null) {
            keyByteMap[key] = bytes
        }

        return bytes
    }

    private fun getFileNameForKey(key: String): String {
        // For now the key and filename are equivalent
        return key
    }

    @Nullable
    private fun getFileByKey(key: String): File? {
        return directory.listFiles()?.find { file ->
            getFileNameForKey(key) == file.name
        }
    }

    private fun getKeyForFileName(fileName: String): String {
        // For now the key and filename are equivalent
        return fileName
    }

    private fun loadAllFiles() {
        directory.listFiles()?.forEach { file ->
            // Populate the cached map
            val key = getKeyForFileName(file.name)
            getBytesInternal(key)
        }
    }

    private fun waitForFilesToLoad() {
        if (isLoadedOrTimedOut) {
            // No need to wait.
            return
        }
        try {
            pendingLoadFuture.get(BACKGROUND_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            // We've made a best effort to load the data in the background. We can simply proceed
            // here.
        }
        isLoadedOrTimedOut = true
    }
}