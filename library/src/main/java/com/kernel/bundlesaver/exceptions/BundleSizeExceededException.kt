package com.kernel.bundlesaver.exceptions

/**
 * Custom exception thrown when the size of a Bundle exceeds the specified limit.
 */
class BundleSizeExceededException(message: String) : Exception(message)