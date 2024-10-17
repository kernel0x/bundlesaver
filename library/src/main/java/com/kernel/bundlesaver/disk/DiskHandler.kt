package com.kernel.bundlesaver.disk

/**
 * A simple interface for managing the storage and retrieval of data to and from disk. All calls
 * should be assumed to be blocking.
 */
internal interface DiskHandler {
    /**
     * Clears any saved data associated with the given `key`.
     *
     * @param key The key for the saved data to clear.
     */
    fun clear(key: String)

    /**
     * Clears all saved data currently stored by the handler.
     */
    fun clearAll()

    /**
     * Retrieves the saved data associated with the given `key` as a byte array (or
     * `null` if there is no data available).
     *
     * @param key The key associated with the saved data to be retrieved.
     * @return The saved data as a byte array (or `null` if there is no data available).
     */
    fun getBytes(key: String): ByteArray?

    /**
     * Stores the given `bytes` to disk and associates them with the given `key` for
     * retrieval later.
     *
     * @param key The key to associate with the saved data.
     * @param bytes The data to be saved.
     */
    fun putBytes(key: String, bytes: ByteArray)
}