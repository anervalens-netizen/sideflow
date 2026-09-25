package eu.astancu.sideflow

import android.content.Context
import kotlin.math.max
import kotlin.math.min

object SidebarStyle {
    const val WIDTH_DP = 180
    const val MAX_HEIGHT_DP = 700
    const val RIGHT_MARGIN_DP = 4
    const val HANDLE_TOUCH_WIDTH_DP = 30
    const val HANDLE_STRIPE_WIDTH_DP = 4
    const val HANDLE_HEIGHT_DP = 115
    const val HANDLE_OFFSET_DP = 214
    const val ICON_DP = 44
    const val CELL_MIN_HEIGHT_DP = 64
    const val HEADER_HEIGHT_DP = 24
    const val SECTION_GAP_DP = 5
    const val CELL_GAP_DP = 2
    const val RADIUS_DP = 28
    const val ANIMATION_MS = 140L
    const val PANEL_COLOR = 0xE61A1C1E.toInt()
    const val CARD_COLOR = 0x66585858
    fun dp(context: Context, value: Int): Int = (context.resources.displayMetrics.density * value + .5f).toInt()
    fun dimensions(width: Int, height: Int, desiredWidth: Int, desiredHeight: Int): Pair<Int, Int> =
        min(max(1, width), max(1, desiredWidth)) to min(max(1, height), max(1, desiredHeight))
    fun handleOffset(availableHeight: Int, handleHeight: Int, desiredOffset: Int): Int {
        val maxOffset = ((availableHeight - handleHeight) / 2).coerceAtLeast(0)
        return desiredOffset.coerceIn(-maxOffset, maxOffset)
    }
}
