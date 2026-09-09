package com.shortstoper.app.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Shared null-safe helpers for detector tree traversal.
 *
 * Detectors stay isolated: each supplies its own markers and predicate, while
 * this object only walks the tree and recycles nodes. The root node is owned
 * (and recycled) by the calling detector's finally block.
 */
object AccessibilityTree {

    /**
     * Maximum number of nodes visited in a single traversal. Third-party
     * accessibility trees can be enormous; without a cap, walking them on the
     * service's main thread can stall the service and get it killed by the
     * system ("service not working").
     */
    private const val MAX_VISITS = 1500

    /**
     * Iterative depth-first search returning true as soon as [predicate]
     * matches a node. Iteration (instead of recursion) makes deep Compose trees
     * from Instagram/Snapchat/YouTube safe: a deep tree used to overflow the
     * call stack with a [StackOverflowError], which is not an [Exception] and
     * therefore slipped past every detector's catch and crashed the service.
     *
     * Every child obtained during traversal is recycled exactly once. The root
     * node is owned by the caller and recycled by the caller's finally block.
     */
    fun anyMatch(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): Boolean {
        val stack = java.util.ArrayDeque<AccessibilityNodeInfo>()
        var visits = 0
        return try {
            if (predicate(root)) return true
            stack.addLast(root)

            while (stack.isNotEmpty()) {
                if (visits++ >= MAX_VISITS) return false
                val node = stack.removeLast()

                // Snapshot all children before recycling the node: recycling a
                // node invalidates its children access.
                val children = ArrayList<AccessibilityNodeInfo>()
                try {
                    val childCount = node.childCount
                    for (i in 0 until childCount) {
                        node.getChild(i)?.let { children.add(it) }
                    }
                } catch (_: Throwable) {
                    // Node was invalidated mid-traversal; skip its subtree.
                }

                // Recycle every node we popped, except the caller-owned root.
                if (node !== root) {
                    try {
                        node.recycle()
                    } catch (_: Exception) {
                    }
                }

                for (idx in children.indices) {
                    val child = children[idx]
                    val matched = try {
                        predicate(child)
                    } catch (_: Throwable) {
                        false
                    }
                    if (matched) {
                        // Recycle this child and all not-yet-visited siblings.
                        for (j in idx until children.size) {
                            try {
                                children[j].recycle()
                            } catch (_: Exception) {
                            }
                        }
                        return true
                    }
                    stack.addLast(child)
                }
            }
            false
        } catch (_: Throwable) {
            false
        } finally {
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (node !== root) {
                    try {
                        node.recycle()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    /**
     * Counts every node (including [root]) for which [predicate] is true, using
     * the same safe iterative traversal as [anyMatch]. Useful for signals that
     * are only meaningful by count (e.g. how many nodes mention "Spotlight").
     */
    fun countMatches(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): Int {
        val stack = java.util.ArrayDeque<AccessibilityNodeInfo>()
        var count = 0
        var visits = 0
        try {
            if (try { predicate(root) } catch (_: Throwable) { false }) count++
            stack.addLast(root)

            while (stack.isNotEmpty()) {
                if (visits++ >= MAX_VISITS) break
                val node = stack.removeLast()

                val children = ArrayList<AccessibilityNodeInfo>()
                try {
                    val childCount = node.childCount
                    for (i in 0 until childCount) {
                        node.getChild(i)?.let { children.add(it) }
                    }
                } catch (_: Throwable) {
                }

                if (node !== root) {
                    try {
                        node.recycle()
                    } catch (_: Exception) {
                    }
                }

                for (child in children) {
                    if (try { predicate(child) } catch (_: Throwable) { false }) count++
                    stack.addLast(child)
                }
            }
            return count
        } catch (_: Throwable) {
            return count
        } finally {
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (node !== root) {
                    try {
                        node.recycle()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun viewId(node: AccessibilityNodeInfo): String =
        try { node.viewIdResourceName.orEmpty() } catch (_: Exception) { "" }

    fun text(node: AccessibilityNodeInfo): String =
        try { node.text?.toString().orEmpty() } catch (_: Exception) { "" }

    fun description(node: AccessibilityNodeInfo): String =
        try { node.contentDescription?.toString().orEmpty() } catch (_: Exception) { "" }

    fun className(node: AccessibilityNodeInfo): String =
        try { node.className?.toString().orEmpty() } catch (_: Exception) { "" }

    fun isVisible(node: AccessibilityNodeInfo): Boolean =
        try { node.isVisibleToUser } catch (_: Exception) { false }

    fun isClickable(node: AccessibilityNodeInfo): Boolean =
        try { node.isClickable } catch (_: Exception) { false }

    fun isSelected(node: AccessibilityNodeInfo): Boolean =
        try { node.isSelected } catch (_: Exception) { false }

    /**
     * True when the node's on-screen bounds sit in the bottom strip of the
     * window, where bottom navigation tabs ("Shorts", "Reels", "Spotlight")
     * live. Used to avoid confusing a persistent nav tab with the actual
     * short-form screen.
     */
    fun isInBottomBar(node: AccessibilityNodeInfo, windowHeight: Int): Boolean {
        if (windowHeight <= 0) return false
        val rect = Rect()
        return try {
            node.getBoundsInScreen(rect)
            rect.bottom > (windowHeight * 0.85).toInt()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * True when the node sits in the top strip of the window, where a
     * full-screen feed's title typically lives.
     */
    fun isNearTop(node: AccessibilityNodeInfo, windowHeight: Int): Boolean {
        if (windowHeight <= 0) return false
        val rect = Rect()
        return try {
            node.getBoundsInScreen(rect)
            rect.top < (windowHeight * 0.25).toInt()
        } catch (_: Exception) {
            false
        }
    }

    fun windowHeight(root: AccessibilityNodeInfo): Int {
        val rect = Rect()
        return try {
            root.getBoundsInScreen(rect)
            rect.height()
        } catch (_: Exception) {
            0
        }
    }

    fun windowWidth(root: AccessibilityNodeInfo): Int {
        val rect = Rect()
        return try {
            root.getBoundsInScreen(rect)
            rect.width()
        } catch (_: Exception) {
            0
        }
    }

    /**
     * True when the node occupies a large portion of the window. A short-form
     * player (or a feed item in a vertical pager) is near-fullscreen, whereas a
     * thumbnail, a nav tab, or a section header is small. This is the key
     * discriminator that separates "actually watching Shorts/Reels" from
     * "browsing the normal feed with a Shorts/Reels tab or tray present".
     */
    fun isLarge(node: AccessibilityNodeInfo, windowHeight: Int, windowWidth: Int): Boolean {
        if (windowHeight <= 0 || windowWidth <= 0) return false
        val rect = Rect()
        return try {
            node.getBoundsInScreen(rect)
            // A fullscreen player is BOTH tall and wide. Requiring both excludes
            // full-width-but-short chrome such as the bottom navigation bar
            // (which otherwise matched the width check and caused the nav tabs
            // "Shorts"/"Reels"/"Spotlight" to be misread as a short-form screen).
            //
            // Visibility + on-screen bounds matter too: tab pager apps
            // (Instagram) keep the Reels viewer inflated in the tree even when
            // the user is on the home feed, just translated off-screen. Without
            // these checks that cached-but-invisible viewer would false-positive.
            val onScreen = rect.right > 0 && rect.left < windowWidth &&
                rect.bottom > 0 && rect.top < windowHeight
            val visible = try { node.isVisibleToUser } catch (_: Exception) { false }
            visible && onScreen &&
                rect.height() >= (windowHeight * 0.45).toInt() &&
                rect.width() >= (windowWidth * 0.60).toInt()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * True when at least one node (including [root]) is large enough to be a
     * fullscreen player / vertical pager / feed container.
     */
    fun hasLargeNode(root: AccessibilityNodeInfo, windowHeight: Int, windowWidth: Int): Boolean =
        anyMatch(root) { isLarge(it, windowHeight, windowWidth) }

    /**
     * True when some node near the top of the window (and not in the bottom nav
     * bar) has text or a content description containing one of [keywords]. This
     * is the title-bar signal for a short-form feed ("Shorts", "Reels",
     * "Spotlight"), used as a fallback when view ids are absent/obfuscated.
     */
    fun hasTitleLike(root: AccessibilityNodeInfo, keywords: Set<String>, windowHeight: Int): Boolean =
        anyMatch(root) { node ->
            val label = "${text(node)} ${description(node)}"
            if (!keywords.any { label.contains(it, ignoreCase = true) }) return@anyMatch false
            isNearTop(node, windowHeight) && !isInBottomBar(node, windowHeight)
        }
}