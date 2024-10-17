package com.kernel.bundlesaver

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.kernel.bundlesaver.disk.FileDiskHandler
import java.util.concurrent.Executors

/**
 * Constant representing the default size limit for Bundle.
 */
const val DEFAULT_BUNDLE_SIZE_LIMIT_BYTES = 524288 // 512 КБ

/**
 * A object for avoiding [android.os.TransactionTooLargeException] in Runtime
 */
object BundleManager {

    private val executorService = Executors.newCachedThreadPool()

    private var activityLogger: ActivitySavedStateLogger? = null

    @Volatile
    private var delegate: BundleSaverDelegate? = null

    @JvmStatic
    val isLogging: Boolean
        get() = activityLogger?.isLogging ?: false

    /**
     * Initializes the framework used to save and restore data and route it to a location free from
     * [android.os.TransactionTooLargeException].
     */
    @JvmStatic
    fun initialize(
        application: Application,
        isLogging: Boolean = true,
        logger: Logger? = null,
        priority: Int = Log.DEBUG,
        tag: String = "BundleSaver",
        limitSizeBundle: Int = DEFAULT_BUNDLE_SIZE_LIMIT_BYTES
    ) {
        delegate = BundleSaverDelegate(application, executorService)
        if (isLogging) {
            startLogging(
                application = application,
                formatter = DefaultFormatter(),
                logger = logger ?: DefaultLogger(priority, tag),
                limitSizeBundle = limitSizeBundle
            )
        }
    }

    /**
     * Restores the state of the given target object based on tracking information stored in the
     * given [Bundle]. The actual saved data will be retrieved from a location in memory or
     * stored on disk.
     *
     * It is required to call [initialize] before calling this method.
     */
    @JvmStatic
    fun restoreInstanceState(target: Any, state: Bundle?) {
        checkInitialization()
        delegate?.restoreInstanceState(target, state)
    }

    /**
     * Saves the state of the given target object to a location in memory and disk and stores
     * tracking information in given [Bundle].
     *
     * It is required to call [initialize] before calling this method.
     */
    @JvmStatic
    fun saveInstanceState(target: Any, state: Bundle) {
        checkInitialization()
        delegate?.saveInstanceState(target, state)
    }

    /**
     * Clears any data associated with the given target object that may be stored to disk. This
     * will not affect data stored for restoration after configuration changes. Due to how these
     * changes are monitored.
     *
     * It is required to call [initialize] before calling this method.
     */
    @JvmStatic
    fun clear(target: Any) {
        checkInitialization()
        delegate?.clear(target)
    }

    /**
     * Clears all data from disk and memory. Does not require a call to [initialize].
     */
    @JvmStatic
    fun clearAll(context: Context) {
        delegate?.clearAll() ?: run {
            executorService.execute {
                FileDiskHandler(context, executorService).clearAll()
            }
        }
    }

    /**
     * Start logging size bundle
     */
    @JvmStatic
    fun startLogging(application: Application, formatter: Formatter, logger: Logger, limitSizeBundle: Int) {
        if (activityLogger == null) {
            activityLogger = ActivitySavedStateLogger(
                formatter = formatter,
                logger = logger,
                limitSizeBundle = limitSizeBundle,
                logFragments = true
            )
        }

        val activityLogger = activityLogger ?: return

        if (activityLogger.isLogging) {
            return
        }

        activityLogger.startLogging()
        application.registerActivityLifecycleCallbacks(activityLogger)
    }

    /**
     * Stop all logging
     */
    @JvmStatic
    fun stopLogging(application: Application) {
        val activityLogger = activityLogger ?: return

        if (!activityLogger.isLogging) {
            return
        }

        activityLogger.stopLogging()
        application.unregisterActivityLifecycleCallbacks(activityLogger)
    }

    private fun checkInitialization() {
        delegate ?: throw IllegalStateException(
            "You must first call initialize before calling any other methods"
        )
    }
}