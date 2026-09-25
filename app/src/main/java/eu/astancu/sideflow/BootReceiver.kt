package eu.astancu.sideflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = RuntimeState(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (state.recoverNewBoot()) ServiceControl.start(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (state.enabled && (state.recoverSameBoot() || !state.hasKnownBoot)) ServiceControl.start(context)
            }
        }
    }
}
