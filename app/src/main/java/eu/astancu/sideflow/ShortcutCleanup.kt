package eu.astancu.sideflow

import android.content.Context
import android.content.pm.ShortcutManager

/** Retires only SideFlow's obsolete toggle shortcut after upgrading. */
object ShortcutCleanup {
    private const val ID = "toggle_sidebar"
    fun run(context: Context) {
        val state = context.getSharedPreferences("minimal_runtime", Context.MODE_PRIVATE)
        if (state.getBoolean("obsolete_shortcut_disabled", false)) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        try {
            val exists = manager.dynamicShortcuts.any { it.id == ID } || manager.pinnedShortcuts.any { it.id == ID }
            if (exists) {
                manager.disableShortcuts(listOf(ID), context.getString(R.string.obsolete_shortcut))
                manager.removeDynamicShortcuts(listOf(ID))
            }
            state.edit().putBoolean("obsolete_shortcut_disabled", true).commit()
        } catch (_: RuntimeException) {
            // Retry on the next cold shelf load; no other shortcuts are changed.
        }
    }
}
