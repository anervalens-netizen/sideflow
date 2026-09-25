package eu.astancu.sideflow

import android.app.AppOpsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/** Watches only this application's overlay permission; no polling or additional permission. */
class OverlayPermissionWatcher(context: Context, private val onRevoked: () -> Unit) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(AppOpsManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closed = false
    private var watching = false
    private val listener = AppOpsManager.OnOpChangedListener { operation, changedPackage ->
        if (operation == AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW &&
            (changedPackage == null || changedPackage == app.packageName)) {
            main.post {
                if (!closed && !Settings.canDrawOverlays(app)) onRevoked()
            }
        }
    }

    fun start() {
        manager.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, app.packageName, listener)
        watching = true
    }

    fun close() {
        closed = true
        main.removeCallbacksAndMessages(null)
        if (watching) {
            try { manager.stopWatchingMode(listener) } catch (_: RuntimeException) { }
            watching = false
        }
    }
}
