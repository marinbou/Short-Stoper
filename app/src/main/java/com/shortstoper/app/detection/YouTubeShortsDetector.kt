package com.shortstoper.app.detection

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects YouTube Shorts (the fullscreen vertical video player).
 *
 * YouTube's Shorts player exposes unique view ids that do not appear anywhere
 * else in the app (verified on-device):
 *   - `reel_recycler`        the vertical Shorts RecyclerView
 *   - `reel_player_page_container` the fullscreen player page container
 *
 * Layered detection, each layer a safety net:
 *   1. A large node whose view id is one of the two markers above.
 *   2. A large RecyclerView whose id contains "reel" (rename resilience).
 * The generic "Shorts" text title is NOT used: it also matches the bottom-nav
 * tab and the home-feed shelf header, which caused the home feed to be
 * misread as Shorts.
 */
class YouTubeShortsDetector : ShortFormDetector {

    private val strongIdMarkers = setOf("reel_recycler", "reel_player_page_container")

    override fun isShortFormScreen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        try {
            val height = AccessibilityTree.windowHeight(root)
            val width = AccessibilityTree.windowWidth(root)

            // Layer 1: the two unique Shorts-player ids, large on screen.
            val strongMatch = AccessibilityTree.anyMatch(root) { node ->
                val id = AccessibilityTree.viewId(node)
                strongIdMarkers.any { id.contains(it, ignoreCase = true) } &&
                    AccessibilityTree.isLarge(node, height, width)
            }
            if (strongMatch) return true

            // Layer 2: a large RecyclerView whose id still mentions "reel".
            return AccessibilityTree.anyMatch(root) { node ->
                val id = AccessibilityTree.viewId(node)
                val cls = AccessibilityTree.className(node)
                id.contains("reel", ignoreCase = true) &&
                    cls.contains("RecyclerView") &&
                    AccessibilityTree.isLarge(node, height, width)
            }
        } catch (_: Exception) {
            return false
        } finally {
            try {
                root.recycle()
            } catch (_: Exception) {
            }
        }
    }
}