package com.kernel.bundlesaver

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import com.kernel.bundlesaver.disk.DiskHandler
import com.kernel.bundlesaver.disk.FileDiskHandler
import com.kernel.bundlesaver.utils.Converter
import com.kernel.bundlesaver.wrapper.unwrapOptimizedObjects
import com.kernel.bundlesaver.wrapper.wrapOptimizedObjects
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

private const val TAG = "BundleSaverDelegate"
private const val BACKGROUND_WAIT_TIMEOUT_MS = 5000L
private const val KEY_UUID = "uuid_%s"

internal class BundleSaverDelegate(
    context: Context,
    private val executorService: ExecutorService
) {

    private val diskHandler: DiskHandler = FileDiskHandler(context, executorService)
    private val pendingWriteTasks = CopyOnWriteArrayList<Runnable>()
    private val uuidBundleMap = ConcurrentHashMap<String, Bundle>()
    private val objectUuidMap = WeakHashMap<Any, String>()
    private var activityCount = 0
    private var isClearAllowed = false
    private var isConfigChange = false
    private var isFirstCreateCall = true
    private var pendingWriteTasksLatch: CountDownLatch? = null

    init {
        registerForLifecycleEvents(context)
        context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun clear(target: Any) {
        if (!isClearAllowed) return
        val uuid = objectUuidMap.remove(target) ?: return
        clearDataForUuid(uuid)
    }

    fun clearAll() {
        uuidBundleMap.clear()
        objectUuidMap.clear()
        doInBackground { diskHandler.clearAll() }
    }

    private fun clearDataForUuid(uuid: String) {
        uuidBundleMap.remove(uuid)
        clearDataFromDisk(uuid)
    }

    private fun clearDataFromDisk(uuid: String) {
        doInBackground { diskHandler.clear(uuid) }
    }

    private fun doInBackground(runnable: Runnable) {
        executorService.execute(runnable)
    }

    private fun getKeyForUuid(target: Any): String = String.format(KEY_UUID, target.javaClass.name)

    private fun getOrGenerateUuid(target: Any): String = objectUuidMap.getOrPut(target) { UUID.randomUUID().toString() }

    @Nullable
    private fun getSavedBundleAndUnwrap(uuid: String): Bundle? {
        val bundle = uuidBundleMap[uuid] ?: readFromDisk(uuid) ?: return null
        unwrapOptimizedObjects(bundle)
        clearDataForUuid(uuid)
        return bundle
    }

    @Nullable
    private fun getSavedUuid(target: Any, state: Bundle): String? {
        return objectUuidMap[target] ?: state.getString(getKeyForUuid(target), null)?.also {
            objectUuidMap[target] = it
        }
    }

    private fun isAppInForeground(): Boolean = activityCount > 0 || isConfigChange

    private fun isFreshStart(activity: Activity, savedInstanceState: Bundle?): Boolean {
        if (!isFirstCreateCall) return false
        isFirstCreateCall = false
        if (savedInstanceState != null) return false
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appTasks = activityManager.appTasks
        return appTasks.size == 1 && appTasks[0].taskInfo.numActivities == 1
    }

    private fun queueDiskWritingIfNecessary(uuid: String, bundle: Bundle) {
        val runnable = object : Runnable {
            override fun run() {
                writeToDisk(uuid, bundle)
                if (!uuidBundleMap.containsKey(uuid)) {
                    clearDataFromDisk(uuid)
                }
                pendingWriteTasks.remove(this) // Используем this для удаления текущего Runnable
                if (pendingWriteTasks.isEmpty() && pendingWriteTasksLatch != null) {
                    pendingWriteTasksLatch?.countDown()
                }
            }
        }
        if (pendingWriteTasksLatch == null || pendingWriteTasksLatch?.count == 0L) {
            pendingWriteTasksLatch = CountDownLatch(1)
        }
        pendingWriteTasks.add(runnable)
        doInBackground(runnable)
        if (isAppInForeground()) return
        try {
            pendingWriteTasksLatch?.await(BACKGROUND_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // Interrupted for an unknown reason, simply proceed.
        }
        pendingWriteTasksLatch = null
    }

    @Nullable
    private fun readFromDisk(uuid: String): Bundle? {
        val bytes = diskHandler.getBytes(uuid) ?: return null
        val bundle = Converter.fromBytes(bytes) ?: run {
            Log.e("Bridge", "Unable to properly convert disk-persisted data to a Bundle. Some state loss may occur.")
            return null
        }
        return bundle
    }

    private fun registerForLifecycleEvents(context: Context) {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacksAdapter() {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    isClearAllowed = true
                    isConfigChange = false
                    if (!isFreshStart(activity, savedInstanceState)) return
                    clearAll()
                }

                override fun onActivityDestroyed(activity: Activity) {
                    isClearAllowed = activity.isFinishing
                }

                override fun onActivityPaused(activity: Activity) {
                    isConfigChange = activity.isChangingConfigurations
                }

                override fun onActivityStarted(activity: Activity) {
                    activityCount++
                }

                override fun onActivityStopped(activity: Activity) {
                    activityCount--
                }
            }
        )
    }

    fun restoreInstanceState(target: Any, state: Bundle?) {
        state ?: return
        getSavedUuid(target, state)?.let { uuid ->
            getSavedBundleAndUnwrap(uuid)?.also { state.putAll(it) }
        }
    }

    fun saveInstanceState(target: Any, state: Bundle) {
        val key = getKeyForUuid(target)
        val uuid = getOrGenerateUuid(target)
        state.putString(key, uuid)
        saveToMemoryAndDiskIfNecessary(uuid, state)
        state.clear()
        state.putString(key, uuid)
    }

    private fun saveToMemoryAndDiskIfNecessary(uuid: String, bundle: Bundle) {
        val state = Bundle(bundle)
        wrapOptimizedObjects(state)
        uuidBundleMap[uuid] = state
        queueDiskWritingIfNecessary(uuid, state)
    }

    private fun writeToDisk(uuid: String, bundle: Bundle) {
        val bytes = Converter.toBytes(bundle)
        diskHandler.putBytes(uuid, bytes)
    }
}