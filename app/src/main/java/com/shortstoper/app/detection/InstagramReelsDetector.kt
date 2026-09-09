package com.shortstoper.app.detection

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects Instagram Reels and the Explore/Discovery tab.
 *
 * Instagram keeps every tab of its main ViewPager preloaded in the tree at all
 * times, so `clips_viewer_view_pager` / `clips_video_container` etc. are present
 * even while the user is on the home feed, Direct, or Search. Counting those ids
 * anywhere in the tree therefore produced false positives and closed the app on
 * screens that are not Reels.
 *
 * The reliable signal is the bottom-nav tab itself: a tab node has
 * `isSelected == true` only while that tab is actually showing.
 *
 *   - `clips_tab` selected  -> the Reels feed is showing.
 *   - `search_tab` selected -> the Explore/Discovery tab is showing. That tab is
 *     a mixed grid that also contains Reels, which cannot be told apart from the
 *     images reliably, so we block the whole tab (simplest correct behaviour).
 */
class InstagramReelsDetector : ShortFormDetector {

    private val strongIdMarkers = setOf(
        "clips_viewer_view_pager",
        "clips_video_container",
        "clips_media_component",
        "clips_single_media_component",
        "clips_expanded_touch_view"
    )

    override fun isShortFormScreen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        try {
            // Case 1: the Explore/Discovery tab is selected. Block it outright —
            // it is a grid mixing Reels and images, so the simplest safe policy is
            // to send the user back to the home feed.
            val exploreSelected = AccessibilityTree.anyMatch(root) { node ->
                AccessibilityTree.viewId(node).contains("search_tab", ignoreCase = true) &&
                    AccessibilityTree.isSelected(node)
            }
            if (exploreSelected) return true

            // Case 2: the Reels bottom-nav tab is selected. Without this gate, the
            // preloaded clips viewer matches on feed/Direct/Search too.
            val clipsTabSelected = AccessibilityTree.anyMatch(root) { node ->
                AccessibilityTree.viewId(node).contains("clips_tab", ignoreCase = true) &&
                    AccessibilityTree.isSelected(node)
            }
            if (!clipsTabSelected) return false

            // At least one Reels-player node is visible to the user.
            return AccessibilityTree.anyMatch(root) { node ->
                val id = AccessibilityTree.viewId(node)
                strongIdMarkers.any { id.contains(it, ignoreCase = true) } &&
                    AccessibilityTree.isVisible(node)
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