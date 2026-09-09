package com.shortstoper.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.Manifest
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var toggle: SwitchCompat
    private lateinit var protectionStatus: TextView
    private lateinit var checkYoutube: ImageView
    private lateinit var checkInstagram: ImageView
    private lateinit var checkSnapchat: ImageView

    /** True while the code is setting the switch state, to ignore that event. */
    private var programmaticCheck = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggle = findViewById(R.id.switch_blocker)
        protectionStatus = findViewById(R.id.txt_protection_status)
        checkYoutube = findViewById(R.id.img_check_youtube)
        checkInstagram = findViewById(R.id.img_check_instagram)
        checkSnapchat = findViewById(R.id.img_check_snapchat)

        findViewById<Button>(R.id.btn_enable_accessibility).setOnClickListener {
            openAccessibilitySettings()
        }
        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            openBatteryOptimizationSettings()
        }

        // The accessibility service can only be enabled/disabled by the user in
        // the system settings, so tapping the switch sends them there. The switch
        // itself reflects the real state, refreshed in onResume.
        toggle.setOnCheckedChangeListener { _, _ ->
            if (!programmaticCheck) openAccessibilitySettings()
        }

        maybePromptBatteryOptimization()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        val enabled = isServiceEnabled()
        programmaticCheck = true
        toggle.isChecked = enabled
        programmaticCheck = false
        refreshStatus(enabled)
    }

    private fun refreshStatus(enabled: Boolean) {
        val checkVisibility = if (enabled) ImageView.VISIBLE else ImageView.INVISIBLE
        checkYoutube.visibility = checkVisibility
        checkInstagram.visibility = checkVisibility
        checkSnapchat.visibility = checkVisibility

        protectionStatus.setText(
            if (enabled) R.string.status_protected else R.string.status_not_protected
        )
        protectionStatus.setTextColor(getColor(if (enabled) R.color.success else R.color.text_muted))
    }

    private fun isServiceEnabled(): Boolean {
        val component = ComponentName(this, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val candidates = setOf(component.flattenToString(), component.flattenToShortString())
        return enabled.split(':').any { it in candidates }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {
            // Fallback to general settings if the activity is unavailable.
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                // No settings screen available; ignore.
            }
        }
    }

    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Some devices lack the direct request screen; fall back to the list.
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                // No battery-optimization settings screen available.
            }
        }
    }

    private fun maybePromptBatteryOptimization() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_BATTERY_PROMPT_SHOWN, false)) return
        prefs.edit().putBoolean(KEY_BATTERY_PROMPT_SHOWN, true).apply()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        openBatteryOptimizationSettings()
    }

    /**
     * On Android 13+ the persistent foreground-service notification is only
     * visible if the user grants the notification permission. Showing it is
     * important on MIUI: a visible ongoing notification is what keeps the OEM
     * process killer from reclaiming the accessibility service.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST
        )
    }

    companion object {
        private const val PREFS_NAME = "short_stoper_prefs"
        private const val KEY_BATTERY_PROMPT_SHOWN = "battery_prompt_shown"
        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }
}