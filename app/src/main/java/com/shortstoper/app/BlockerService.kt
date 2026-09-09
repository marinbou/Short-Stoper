package com.shortstoper.app

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.shortstoper.app.detection.InstagramReelsDetector
import com.shortstoper.app.detection.ShortFormDetector
import com.shortstoper.app.detection.SnapchatSpotlightDetector
import com.shortstoper.app.detection.YouTubeShortsDetector

/**
 * The app's single accessibility service. Observes events only for the three
 * configured target packages (see res/xml/accessibility_service_config.xml),
 * dispatches each event to the matching detector, and fires the system Back
 * action with a per-package debounce to prevent loops.
 */
class BlockerService : AccessibilityService() {

    companion object {
        /**
         * Minimum interval between two Back actions for the same package. Kept
         * well under the old 2s: several Backs are usually needed to leave a
         * short-form screen (the first only dismisses an in-player panel), so a
         * short cooldown retries quickly and shortens the total escape time.
         */
        private const val BACK_COOLDOWN_MS = 450L

        /**
         * Consecutive block attempts after which we escalate from Back to Home.
         * A single Back often only closes an overlay, so we give it a couple of
         * tries; if the short-form screen is still detected afterwards, Home is
         * the guaranteed way out (radical, but only used as a last resort).
         */
        private const val ESCALATE_AFTER_ATTEMPTS = 3

        /** Logcat tag used by debug-only diagnostics. */
        private const val TAG = "ShortStoper"

        /** Foreground-service notification id (keeps the process alive on MIUI). */
        private const val NOTIFICATION_ID = 1

        /** Notification channel id for the persistent protection notification. */
        private const val NOTIFICATION_CHANNEL_ID = "protection"

        /** Minimum interval between two diagnostic dumps for the same package. */
        private const val DEBUG_LOG_INTERVAL_MS = 2000L

        /**
         * Minimum interval between two full detection passes for the same
         * package. `TYPE_WINDOW_CONTENT_CHANGED` can fire dozens of times a
         * second; without throttling, the main-thread tree walk can stall the
         * service and get it killed.
         */
        private const val EVENT_THROTTLE_MS = 250L

        /** Text markers whose presence is counted (never the text itself). */
        private val DEBUG_MARKERS = arrayOf("shorts", "reel", "clips", "spotlight")
    }

    /**
     * Central package -> detector registry. Adding a new platform requires only
     * a new [ShortFormDetector] implementation plus one entry here; existing
     * detectors are untouched (extensibility contract).
     */
    private val detectors: Map<String, ShortFormDetector> = mapOf(
        "com.google.android.youtube" to YouTubeShortsDetector(),
        "com.instagram.android" to InstagramReelsDetector(),
        "com.snapchat.android" to SnapchatSpotlightDetector()
    )

    /** Last Back-action time per package (monotonic clock). */
    private val lastActionTimes = mutableMapOf<String, Long>()

    /** Consecutive block attempts per package while still on short-form. */
    private val consecutiveBlocks = mutableMapOf<String, Int>()

    /** Last diagnostic dump time per package (monotonic clock). */
    private val debugLogTimes = mutableMapOf<String, Long>()

    /** Last full detection pass time per package (monotonic clock). */
    private val lastProcessTimes = mutableMapOf<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Belt and braces: a malformed node tree from a third-party app must
        // never be able to take down the service (the system would then show
        // "this service is not working").
        try {
            handleEvent(event)
        } catch (t: Throwable) {
            Log.w(TAG, "unhandled event failure", t)
        }
    }

    private fun handleEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val detector = detectors[packageName] ?: return

        val now = SystemClock.elapsedRealtime()

        // Throttle: skip redundant bursts of content-changed events for the
        // same package so the main thread is not saturated by tree walks.
        val lastProcess = lastProcessTimes[packageName]
        if (lastProcess != null && now - lastProcess < EVENT_THROTTLE_MS) return
        lastProcessTimes[packageName] = now

        val root = rootInActiveWindow ?: return

        // The detector is responsible for recycling root (guaranteed via
        // try/finally inside every detector).
        val isShortForm = try {
            detector.isShortFormScreen(root)
        } catch (t: Throwable) {
            false
        }

        logDebugDecision(packageName, isShortForm, now)

        if (isShortForm) {
            // Timed cooldown (not a permanent lock): the first Back may only
            // dismiss an in-player panel (comments/likes), so we keep trying
            // every BACK_COOLDOWN_MS until the short-form screen actually exits.
            val last = lastActionTimes[packageName]
            if (last == null || now - last >= BACK_COOLDOWN_MS) {
                lastActionTimes[packageName] = now

                val attempts = (consecutiveBlocks[packageName] ?: 0) + 1
                consecutiveBlocks[packageName] = attempts

                // Escalate to Home after several Backs failed to leave the
                // short-form screen. Home exits the whole app, but guarantees
                // the user is no longer stuck in the short-form player.
                if (attempts >= ESCALATE_AFTER_ATTEMPTS) {
                    consecutiveBlocks[packageName] = 0
                    val ok = performGlobalAction(GLOBAL_ACTION_HOME)
                    if (isDebuggable()) {
                        Log.d(TAG, "pkg=$packageName HOME fired ok=$ok (escalated after $attempts attempts)")
                    }
                } else {
                    val ok = performGlobalAction(GLOBAL_ACTION_BACK)
                    if (isDebuggable()) {
                        Log.d(TAG, "pkg=$packageName BACK fired ok=$ok (attempt $attempts)")
                    }
                }
            }
        } else {
            // Left short-form: reset the escalation counter.
            consecutiveBlocks[packageName] = 0
        }
    }

    /**
     * Debug-only diagnostics for tuning detection on a real device. Enabled only
     * for debuggable builds (sideloaded debug APKs), rate-limited per package,
     * and limited to structural metadata: view ids, class names, and the *count*
     * of nodes whose text mentions each marker. No user-visible text or content
     * is ever logged.
     */
    private fun logDebugDecision(packageName: String, isShortForm: Boolean, now: Long) {
        if (!isDebuggable()) return

        val last = debugLogTimes[packageName]
        if (last != null && now - last < DEBUG_LOG_INTERVAL_MS) return
        debugLogTimes[packageName] = now

        val root = rootInActiveWindow ?: return
        try {
            val ids = linkedSetOf<String>()
            val classes = linkedSetOf<String>()
            val markerCounts = mutableMapOf<String, Int>()
            val selectedIds = linkedSetOf<String>()
            collectMeta(root, ids, classes, markerCounts, selectedIds, 0)

            Log.d(
                TAG,
                "pkg=$packageName decision=${if (isShortForm) "BLOCK" else "ALLOW"} " +
                    "markers=$markerCounts ids=$ids"
            )
            Log.d(TAG, "pkg=$packageName selected=$selectedIds classes=$classes")
        } finally {
            try {
                root.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun collectMeta(
        node: AccessibilityNodeInfo,
        ids: MutableSet<String>,
        classes: MutableSet<String>,
        markerCounts: MutableMap<String, Int>,
        selectedIds: MutableSet<String>,
        depth: Int
    ) {
        if (depth > 10 || ids.size > 200 || classes.size > 300) return

        try {
            node.viewIdResourceName?.takeIf { it.isNotEmpty() }?.let { ids.add(it) }
            node.className?.toString()?.takeIf { it.isNotEmpty() }?.let { classes.add(it) }

            val id = node.viewIdResourceName.orEmpty()
            if (id.isNotEmpty() && node.isSelected) {
                selectedIds.add(id)
            }

            val text = node.text?.toString().orEmpty()
            val description = node.contentDescription?.toString().orEmpty()
            val combined = "$text $description"
            for (marker in DEBUG_MARKERS) {
                if (combined.contains(marker, ignoreCase = true)) {
                    markerCounts[marker] = markerCounts.getOrDefault(marker, 0) + 1
                }
            }
        } catch (_: Exception) {
            return
        }

        val childCount = try {
            node.childCount
        } catch (_: Exception) {
            0
        }
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            } ?: continue
            try {
                collectMeta(child, ids, classes, markerCounts, selectedIds, depth + 1)
            } finally {
                try {
                    child.recycle()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onInterrupt() {
        // Required no-op override; no state to clean up.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lastActionTimes.clear()
        consecutiveBlocks.clear()
        lastProcessTimes.clear()
        debugLogTimes.clear()

        // Promote the service to a foreground service so aggressive OEM process
        // killers (MIUI/HyperOS "SwipeUpClean", EMUI, etc.) do not kill the
        // process and silently stop the accessibility service with it.
        promoteToForeground()

        if (isDebuggable()) {
            Log.d(TAG, "service connected")
        }
    }

    /**
     * Runs the service as a foreground service with a minimal, ongoing
     * notification. This is the most reliable defense against MIUI killing the
     * process when the app is swiped away from Recents or reclaimed in the
     * background. On devices where [ServiceCompat.startForeground] is rejected
     * the accessibility service still works normally — it is just less
     * protected from OEM process killers.
     */
    private fun promoteToForeground() {
        try {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_protect)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } catch (t: Throwable) {
            Log.w(TAG, "could not promote to foreground service", t)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_text)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}