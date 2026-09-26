package eu.astancu.sideflow

import android.content.Context

/** Minimal, explicit handle geometry settings. Defaults preserve the accepted SideFlow Minimal layout. */
class HandlePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("minimal_handle", Context.MODE_PRIVATE)

    var heightDp: Int
        get() = SidebarStyle.handleHeight(prefs.getInt(KEY_HEIGHT, SidebarStyle.HANDLE_HEIGHT_DP))
        set(value) { prefs.edit().putInt(KEY_HEIGHT, SidebarStyle.handleHeight(value)).apply() }

    var offsetDp: Int
        get() = SidebarStyle.desiredHandleOffset(prefs.getInt(KEY_OFFSET, SidebarStyle.HANDLE_OFFSET_DP))
        set(value) { prefs.edit().putInt(KEY_OFFSET, SidebarStyle.desiredHandleOffset(value)).apply() }

    companion object {
        private const val KEY_HEIGHT = "height_dp"
        private const val KEY_OFFSET = "offset_dp"
    }
}
