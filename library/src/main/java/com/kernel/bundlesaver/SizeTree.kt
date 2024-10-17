package com.kernel.bundlesaver

/**
 * Data class representing a hierarchical structure of items with associated sizes.
 * Each node in the tree has a unique key, a total size, and a list of subtrees.
 */
internal data class SizeTree(
    val key: String,
    val totalSize: Int,
    val subTrees: List<SizeTree> = emptyList()
)