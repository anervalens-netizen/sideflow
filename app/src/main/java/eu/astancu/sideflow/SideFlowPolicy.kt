package eu.astancu.sideflow

/**
 * Pure policy helpers kept Android-free so layout and launch behavior can be
 * regression-tested without an emulator.
 */
object SideFlowPolicy {
    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 6
    const val DEFAULT_COLUMNS = 4

    const val MIN_PANEL_WIDTH_DP = 180
    const val MAX_PANEL_WIDTH_DP = 420
    const val DEFAULT_PANEL_WIDTH_DP = 300
    const val PICKER_COLLAPSED_PANEL_WIDTH_DP = 88

    const val MIN_ITEM_GAP_DP = 0
    const val MAX_ITEM_GAP_DP = 20
    const val DEFAULT_ITEM_GAP_DP = 6

    fun sanitizeColumns(value: Int): Int = value.coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    fun sanitizePanelWidthDp(value: Int): Int =
        value.coerceIn(MIN_PANEL_WIDTH_DP, MAX_PANEL_WIDTH_DP)

    fun sanitizeItemGapDp(value: Int): Int =
        value.coerceIn(MIN_ITEM_GAP_DP, MAX_ITEM_GAP_DP)

    /**
     * Normal taps never request freeform. Freeform is reserved for an explicit
     * secondary action such as drag-to-freeform.
     */
    fun shouldLaunchFreeform(explicitSecondaryAction: Boolean, freeformAvailable: Boolean): Boolean =
        explicitSecondaryAction && freeformAvailable
}
