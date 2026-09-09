package com.shortstoper.app.detection

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Common contract implemented by every platform detector.
 *
 * Implementations MUST be null-safe, MUST NOT throw, and MUST recycle every
 * AccessibilityNodeInfo they obtain (including [root]) before returning.
 */
interface ShortFormDetector {

    /**
     * Returns true when [root] represents this platform's short-form screen
     * (e.g. YouTube Shorts, Instagram Reels, Snapchat Spotlight).
     *
     * Returns false for null input and for any failure during traversal.
     */
    fun isShortFormScreen(root: AccessibilityNodeInfo?): Boolean
}