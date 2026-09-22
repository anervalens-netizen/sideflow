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
    const val PANEL_WIDTH_STEP_DP = 10
    const val PICKER_COLLAPSED_PANEL_WIDTH_DP = 88

    const val MIN_ITEM_GAP_DP = 0
    const val MAX_ITEM_GAP_DP = 20
    const val DEFAULT_ITEM_GAP_DP = 6

    const val MIN_BLUR_AMOUNT = 5
    const val MAX_BLUR_AMOUNT = 50
    const val MIN_PANEL_OPACITY = 10
    const val MAX_PANEL_OPACITY = 100
    const val MIN_PANEL_RADIUS_DP = 0
    const val MAX_PANEL_RADIUS_DP = 60
    const val MIN_PANEL_MAX_HEIGHT_DP = 200
    const val MAX_PANEL_MAX_HEIGHT_DP = 800
    const val MIN_PICKER_MAX_HEIGHT_DP = 300
    const val MAX_PICKER_MAX_HEIGHT_DP = 800
    const val MIN_ICON_SCALE = 0.8f
    const val MAX_ICON_SCALE = 2.0f

    private const val PANEL_GRID_HORIZONTAL_INSET_DP = 16
    private const val CELL_SAFETY_DP = 1

    data class WindowBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    fun sanitizeColumns(value: Int): Int = value.coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    fun sanitizePanelWidthDp(value: Int): Int {
        val clamped = value.coerceIn(MIN_PANEL_WIDTH_DP, MAX_PANEL_WIDTH_DP)
        val offset = clamped - MIN_PANEL_WIDTH_DP
        val roundedSteps = (offset + PANEL_WIDTH_STEP_DP / 2) / PANEL_WIDTH_STEP_DP
        return (MIN_PANEL_WIDTH_DP + roundedSteps * PANEL_WIDTH_STEP_DP)
            .coerceIn(MIN_PANEL_WIDTH_DP, MAX_PANEL_WIDTH_DP)
    }

    fun sanitizeItemGapDp(value: Int): Int =
        value.coerceIn(MIN_ITEM_GAP_DP, MAX_ITEM_GAP_DP)

    fun sanitizeBlurAmount(value: Int): Int =
        value.coerceIn(MIN_BLUR_AMOUNT, MAX_BLUR_AMOUNT)

    fun sanitizePanelOpacity(value: Int): Int =
        value.coerceIn(MIN_PANEL_OPACITY, MAX_PANEL_OPACITY)

    fun sanitizePanelRadiusDp(value: Int): Int =
        value.coerceIn(MIN_PANEL_RADIUS_DP, MAX_PANEL_RADIUS_DP)

    fun sanitizePanelMaxHeightDp(value: Int): Int =
        value.coerceIn(MIN_PANEL_MAX_HEIGHT_DP, MAX_PANEL_MAX_HEIGHT_DP)

    fun sanitizePickerMaxHeightDp(value: Int): Int =
        value.coerceIn(MIN_PICKER_MAX_HEIGHT_DP, MAX_PICKER_MAX_HEIGHT_DP)

    fun sanitizeIconScale(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_ICON_SCALE, MAX_ICON_SCALE) else 1.0f

    fun applyOpacityToArgb(argb: Int, opacityPercent: Int): Int {
        val baseAlpha = (argb ushr 24) and 0xFF
        val opacity = sanitizePanelOpacity(opacityPercent)
        val scaledAlpha = (baseAlpha * opacity + 50) / 100
        return (argb and 0x00FFFFFF) or (scaledAlpha shl 24)
    }

    /**
     * Keeps an icon inside its actual grid cell even at the narrowest panel /
     * highest-column combination. The requested visual scale is preserved when
     * there is enough room and reduced only when required to avoid clipping.
     */
    fun fitIconSizeDp(
        panelWidthDp: Int,
        columns: Int,
        requestedIconDp: Int,
        itemGapDp: Int,
        contentHorizontalPaddingDp: Int = 0
    ): Int {
        val safeColumns = sanitizeColumns(columns)
        val safeWidth = sanitizePanelWidthDp(panelWidthDp)
        val safeGap = sanitizeItemGapDp(itemGapDp)
        val usableGridWidth = (safeWidth - PANEL_GRID_HORIZONTAL_INSET_DP).coerceAtLeast(safeColumns)
        val cellWidth = usableGridWidth / safeColumns
        val maxIcon = (
            cellWidth -
                safeGap -
                contentHorizontalPaddingDp.coerceAtLeast(0) -
                CELL_SAFETY_DP
            ).coerceAtLeast(1)
        return requestedIconDp.coerceAtLeast(1).coerceAtMost(maxIcon)
    }

    /**
     * A section-header drop targets the first slot after that header. If the
     * dragged item originates before the header, removing it shifts the header
     * one position left before insertion.
     */
    fun sectionHeaderDropInsertionIndex(
        fromIndex: Int,
        headerIndexBeforeRemoval: Int,
        itemCountBeforeRemoval: Int
    ): Int {
        if (itemCountBeforeRemoval <= 1) return 0
        val safeFrom = fromIndex.coerceIn(0, itemCountBeforeRemoval - 1)
        val safeHeader = headerIndexBeforeRemoval.coerceIn(0, itemCountBeforeRemoval - 1)
        val shiftedHeader = if (safeFrom < safeHeader) safeHeader - 1 else safeHeader
        val remainingCount = itemCountBeforeRemoval - 1
        return (shiftedHeader + 1).coerceIn(0, remainingCount)
    }

    /**
     * Computes the same configured freeform geometry for every explicit
     * freeform launch path, including center-drop.
     */
    fun freeformBounds(
        screenWidthPx: Int,
        screenHeightPx: Int,
        mode: String,
        customWidthPercent: Int,
        customHeightPercent: Int
    ): WindowBounds {
        val width = screenWidthPx.coerceAtLeast(1)
        val height = screenHeightPx.coerceAtLeast(1)

        return when (mode) {
            "portrait" -> {
                val left = width / 3
                val top = height / 15
                WindowBounds(left, top, width - left, height - top)
            }
            "maximized" -> WindowBounds(0, 0, width, height)
            "custom" -> {
                val widthPercent = customWidthPercent.coerceIn(20, 100)
                val heightPercent = customHeightPercent.coerceIn(20, 100)
                val windowWidth = (width * widthPercent / 100.0).toInt()
                val windowHeight = (height * heightPercent / 100.0).toInt()
                val left = (width - windowWidth) / 2
                val top = (height - windowHeight) / 2
                WindowBounds(left, top, left + windowWidth, top + windowHeight)
            }
            else -> {
                val horizontalMargin = width / 10
                val verticalMargin = height / 10
                WindowBounds(
                    horizontalMargin,
                    verticalMargin,
                    width - horizontalMargin,
                    height - verticalMargin
                )
            }
        }
    }

    fun shouldSeedDefaultShelf(hasExistingConfiguration: Boolean): Boolean =
        !hasExistingConfiguration

    /**
     * Normal taps never request freeform. Freeform is reserved for an explicit
     * secondary action such as drag-to-freeform.
     */
    fun shouldLaunchFreeform(
        explicitSecondaryAction: Boolean,
        freeformAvailable: Boolean
    ): Boolean = explicitSecondaryAction && freeformAvailable

    /**
     * v1 windowing drag intentionally supports only ordinary app launches.
     * URL/deep-link/activity intents must keep their full target and therefore
     * stay on the normal tap path until drag carries structured intents.
     */
    fun canUseWindowingDrag(isPlainApp: Boolean, hasIntentUri: Boolean): Boolean =
        isPlainApp && !hasIntentUri
}
