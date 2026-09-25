package eu.astancu.sideflow

import android.content.Context
import android.content.Intent
import android.provider.Settings

object RecoveryPolicy {
    fun sameBoot(enabled: Boolean, currentBoot: Int, storedBoot: Int, overlayAllowed: Boolean): Boolean =
        enabled && overlayAllowed && currentBoot >= 0 && currentBoot == storedBoot

    fun newBoot(enabled: Boolean, autoStart: Boolean, currentBoot: Int, storedBoot: Int, overlayAllowed: Boolean): Boolean =
        enabled && autoStart && overlayAllowed && currentBoot >= 0 && currentBoot != storedBoot
}

/** Durable intent state shared by editor, notification, boot and accessibility recovery. */
class RuntimeState(context: Context) {
    private val app = context.applicationContext
    private val state = app.getSharedPreferences("minimal_runtime", Context.MODE_PRIVATE)
    private val legacy = app.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE)
    init {
        if (!state.contains("enabled")) {
            state.edit().putBoolean("enabled", legacy.getBoolean("service_enabled", false))
                .putInt("boot", legacy.getInt("last_recovery_boot_count", -1)).commit()
        }
    }
    val enabled: Boolean get() = state.getBoolean("enabled", false)
    val hasKnownBoot: Boolean get() = storedBoot >= 0
    // Original PanelPreferences.DEFAULT_AUTO_START was true.
    val autoStart: Boolean get() = legacy.getBoolean("auto_start", true)
    val bootCount: Int get() = runCatching { Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)
    val storedBoot: Int get() = state.getInt("boot", legacy.getInt("last_recovery_boot_count", -1))
    val error: String? get() = state.getString("error", null)

    fun setEnabled(value: Boolean): Boolean {
        val saved = state.edit().putBoolean("enabled", value).putInt("boot", bootCount).remove("error").commit()
        if (!saved) setError("Could not save sidebar state")
        return saved
    }
    fun markBoot(): Boolean = state.edit().putInt("boot", bootCount).commit()
    fun setError(message: String) { state.edit().putString("error", message.take(240)).apply() }
    fun clearError() { state.edit().remove("error").apply() }
    fun recoverSameBoot(): Boolean = RecoveryPolicy.sameBoot(enabled, bootCount, storedBoot, Settings.canDrawOverlays(app))
    fun recoverNewBoot(): Boolean = RecoveryPolicy.newBoot(enabled, autoStart, bootCount, storedBoot, Settings.canDrawOverlays(app))
}

object ServiceControl {
    fun start(context: Context): Boolean {
        val state = RuntimeState(context)
        if (!state.enabled) return false
        if (!Settings.canDrawOverlays(context)) {
            state.setError("Allow display over other apps, then tap Start.")
            return false
        }
        return try {
            val intent = Intent(context, FloatingPanelService::class.java)
            if (FloatingPanelService.isRunning) return true
            context.startForegroundService(intent)
            true
        } catch (failure: RuntimeException) {
            state.setError("Could not start sidebar: ${failure.javaClass.simpleName}")
            false
        }
    }

    fun stop(context: Context): Boolean {
        val state = RuntimeState(context)
        if (!state.setEnabled(false)) return false
        return try {
            context.stopService(Intent(context, FloatingPanelService::class.java))
            true
        } catch (failure: RuntimeException) {
            state.setError("Sidebar state was saved off, but service stop failed: ${failure.javaClass.simpleName}")
            false
        }
    }
}
