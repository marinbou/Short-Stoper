package com.shortstoper.app.detection

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects Snapchat Spotlight (the fullscreen vertical video feed).
 *
 * Snapchat's tree is almost entirely Jetpack Compose (`javaClass` nodes), so
 * classic RecyclerView/ViewPager view ids do not exist on Spotlight. Instead we
 * use signals verified on-device:
 *   - the camera screen always exposes `camera_capture_button` — if present we
 *     are NOT in Spotlight (and must not block, or the app becomes unusable);
 *   - Spotlight injects ad components (`sc_composer_*`) and a "favorite" id;
 *   - the word "spotlight" appears twice on Spotlight (title + card) but only
 *     once on the camera (the nav label).
 *
 * The camera veto is checked FIRST: any block condition is ignored while the
 * camera is up, which is what makes the camera usable while still blocking
 * Spotlight.
 */
class SnapchatSpotlightDetector : ShortFormDetector {

    override fun isShortFormScreen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        try {
            // Veto: the camera screen is never short-form content.
            val cameraVisible = AccessibilityTree.anyMatch(root) { node ->
                val id = AccessibilityTree.viewId(node)
                id.contains("camera_capture_button", ignoreCase = true) ||
                    id.contains("camera_capture", ignoreCase = true)
            }
            if (cameraVisible) return false

            // Layer 1: Spotlight ad / player components.
            val adMatch = AccessibilityTree.anyMatch(root) { node ->
                val id = AccessibilityTree.viewId(node)
                (id.contains("sc_composer", ignoreCase = true) ||
                    id.contains("favorite", ignoreCase = true)) &&
                    AccessibilityTree.isVisible(node)
            }
            if (adMatch) return true

            // Layer 2: the word "spotlight" appears more than once (title +
            // card), which only happens on the Spotlight feed, never on camera.
            return AccessibilityTree.countMatches(root) { node ->
                val label = "${AccessibilityTree.text(node)} ${AccessibilityTree.description(node)}"
                label.contains("spotlight", ignoreCase = true)
            } >= 2
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