package eu.astancu.sideflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SideFlowPolicyTest {

    @Test
    fun columnsAreClampedToSupportedRange() {
        assertEquals(2, SideFlowPolicy.sanitizeColumns(1))
        assertEquals(2, SideFlowPolicy.sanitizeColumns(2))
        assertEquals(4, SideFlowPolicy.sanitizeColumns(4))
        assertEquals(6, SideFlowPolicy.sanitizeColumns(6))
        assertEquals(6, SideFlowPolicy.sanitizeColumns(9))
    }

    @Test
    fun panelWidthIsClampedToSafeRange() {
        assertEquals(180, SideFlowPolicy.sanitizePanelWidthDp(40))
        assertEquals(300, SideFlowPolicy.sanitizePanelWidthDp(300))
        assertEquals(420, SideFlowPolicy.sanitizePanelWidthDp(900))
    }

    @Test
    fun itemGapIsClampedToSafeRange() {
        assertEquals(0, SideFlowPolicy.sanitizeItemGapDp(-5))
        assertEquals(6, SideFlowPolicy.sanitizeItemGapDp(6))
        assertEquals(20, SideFlowPolicy.sanitizeItemGapDp(99))
    }

    @Test
    fun panelWidthIsQuantizedToSliderStep() {
        assertEquals(180, SideFlowPolicy.sanitizePanelWidthDp(184))
        assertEquals(190, SideFlowPolicy.sanitizePanelWidthDp(185))
        assertEquals(190, SideFlowPolicy.sanitizePanelWidthDp(191))
        assertEquals(420, SideFlowPolicy.sanitizePanelWidthDp(419))
    }

    @Test
    fun iconsFitInsideNarrowSixColumnCells() {
        val fitted = SideFlowPolicy.fitIconSizeDp(
            panelWidthDp = 180,
            columns = 6,
            requestedIconDp = 40,
            itemGapDp = 6
        )
        assertEquals(20, fitted)
        assertEquals(
            40,
            SideFlowPolicy.fitIconSizeDp(300, 4, 40, 6)
        )
    }


    @Test
    fun richIconsAccountForRecyclerAndInnerPadding() {
        assertEquals(
            12,
            SideFlowPolicy.fitIconSizeDp(
                panelWidthDp = 180,
                columns = 6,
                requestedIconDp = 44,
                itemGapDp = 6,
                contentHorizontalPaddingDp = 8
            )
        )
    }

    @Test
    fun blurAmountStaysInsideSliderRange() {
        assertEquals(5, SideFlowPolicy.sanitizeBlurAmount(0))
        assertEquals(28, SideFlowPolicy.sanitizeBlurAmount(28))
        assertEquals(50, SideFlowPolicy.sanitizeBlurAmount(99))
    }

    @Test
    fun appearanceSliderValuesAreSanitized() {
        assertEquals(10, SideFlowPolicy.sanitizePanelOpacity(-1))
        assertEquals(100, SideFlowPolicy.sanitizePanelOpacity(150))
        assertEquals(0, SideFlowPolicy.sanitizePanelRadiusDp(-2))
        assertEquals(60, SideFlowPolicy.sanitizePanelRadiusDp(80))
        assertEquals(0.8f, SideFlowPolicy.sanitizeIconScale(0.1f), 0.0001f)
        assertEquals(1.0f, SideFlowPolicy.sanitizeIconScale(1.04f), 0.0001f)
        assertEquals(1.1f, SideFlowPolicy.sanitizeIconScale(1.05f), 0.0001f)
        assertEquals(2.0f, SideFlowPolicy.sanitizeIconScale(4.0f), 0.0001f)
        assertEquals(200, SideFlowPolicy.sanitizePanelMaxHeightDp(10))
        assertEquals(800, SideFlowPolicy.sanitizePanelMaxHeightDp(900))
        assertEquals(300, SideFlowPolicy.sanitizePickerMaxHeightDp(10))
        assertEquals(800, SideFlowPolicy.sanitizePickerMaxHeightDp(900))
    }

    @Test
    fun folderChildrenAreNotAdvertisedAsReorderable() {
        assertTrue(SideFlowPolicy.canReorderShelfItem(false, true, "apps"))
        assertFalse(SideFlowPolicy.canReorderShelfItem(false, true, "folder:parent-id"))
        assertFalse(SideFlowPolicy.canReorderShelfItem(true, true, "apps"))
        assertFalse(SideFlowPolicy.canReorderShelfItem(false, false, "apps"))
    }

    @Test
    fun translucentLightSurfaceUsesLightContentForUnknownBackdrop() {
        assertTrue(SideFlowPolicy.shouldUseDarkContentForSurface(true, 255, false))
        assertTrue(SideFlowPolicy.shouldUseDarkContentForSurface(true, 192, false))
        assertFalse(SideFlowPolicy.shouldUseDarkContentForSurface(true, 191, false))
        assertFalse(SideFlowPolicy.shouldUseDarkContentForSurface(false, 255, false))
        assertFalse(SideFlowPolicy.shouldUseDarkContentForSurface(true, 255, true))
        assertFalse(
            SideFlowPolicy.shouldUseDarkContentForSurface(
                surfaceIsLight = true,
                effectiveAlpha = 255,
                hideBackground = false,
                forceDarkSurface = true
            )
        )
    }

    @Test
    fun legacyOnlyEmptyShelfStateIsUninitialized() {
        assertFalse(SideFlowPolicy.hasShelfConfiguration(false, false, false))
        assertFalse(SideFlowPolicy.hasShelfConfiguration(false, true, false))
        assertTrue(SideFlowPolicy.hasShelfConfiguration(false, true, true))
        assertTrue(SideFlowPolicy.hasShelfConfiguration(true, true, false))
    }

    @Test
    fun pseudoIconPaddingAlwaysLeavesDrawableSpace() {
        assertEquals(3, SideFlowPolicy.pseudoIconPaddingDp(12))
        assertEquals(8, SideFlowPolicy.pseudoIconPaddingDp(40))
        assertEquals(0, SideFlowPolicy.pseudoIconPaddingDp(6))
        assertEquals(1, SideFlowPolicy.pseudoIconPaddingDp(8))
    }

    @Test
    fun panelOpacityIsTheFinalBackgroundAlpha() {
        assertEquals(0x80112233.toInt(), SideFlowPolicy.applyOpacityToArgb(0xFF112233.toInt(), 50))
        assertEquals(0x80112233.toInt(), SideFlowPolicy.applyOpacityToArgb(0x80112233.toInt(), 50))
        assertEquals(0x80112233.toInt(), SideFlowPolicy.applyOpacityToArgb(0x33112233.toInt(), 50))
        assertEquals(0xFF112233.toInt(), SideFlowPolicy.applyOpacityToArgb(0xE6112233.toInt(), 100))
        assertEquals(0x1A112233.toInt(), SideFlowPolicy.applyOpacityToArgb(0xE6112233.toInt(), 10))
    }

    @Test
    fun downwardHeaderDropAccountsForSourceRemoval() {
        assertEquals(
            2,
            SideFlowPolicy.sectionHeaderDropInsertionIndex(
                fromIndex = 1,
                headerIndexBeforeRemoval = 2,
                itemCountBeforeRemoval = 4
            )
        )
        assertEquals(
            1,
            SideFlowPolicy.sectionHeaderDropInsertionIndex(
                fromIndex = 3,
                headerIndexBeforeRemoval = 0,
                itemCountBeforeRemoval = 4
            )
        )
    }

    @Test
    fun configuredFreeformBoundsAreSharedAcrossModes() {
        assertEquals(
            SideFlowPolicy.WindowBounds(100, 200, 900, 1800),
            SideFlowPolicy.freeformBounds(1000, 2000, "standard", 80, 80)
        )
        assertEquals(
            SideFlowPolicy.WindowBounds(333, 133, 667, 1867),
            SideFlowPolicy.freeformBounds(1000, 2000, "portrait", 80, 80)
        )
        assertEquals(
            SideFlowPolicy.WindowBounds(0, 0, 1000, 2000),
            SideFlowPolicy.freeformBounds(1000, 2000, "maximized", 80, 80)
        )
        assertEquals(
            SideFlowPolicy.WindowBounds(250, 400, 750, 1600),
            SideFlowPolicy.freeformBounds(1000, 2000, "custom", 50, 60)
        )
    }

    @Test
    fun windowingDragRejectsStructuredIntentTargets() {
        assertTrue(SideFlowPolicy.canUseWindowingDrag(isPlainApp = true, hasIntentUri = false))
        assertFalse(SideFlowPolicy.canUseWindowingDrag(isPlainApp = true, hasIntentUri = true))
        assertFalse(SideFlowPolicy.canUseWindowingDrag(isPlainApp = false, hasIntentUri = false))
    }

    @Test
    fun defaultShelfSeedingOnlyHappensWithoutExistingConfiguration() {
        assertTrue(SideFlowPolicy.shouldSeedDefaultShelf(hasExistingConfiguration = false))
        assertFalse(SideFlowPolicy.shouldSeedDefaultShelf(hasExistingConfiguration = true))
    }

    @Test
    fun ordinaryTapNeverRequestsFreeform() {
        assertFalse(SideFlowPolicy.shouldLaunchFreeform(false, true))
        assertFalse(SideFlowPolicy.shouldLaunchFreeform(false, false))
    }

    @Test
    fun freeformRequiresExplicitSecondaryActionAndAvailability() {
        assertFalse(SideFlowPolicy.shouldLaunchFreeform(true, false))
        assertTrue(SideFlowPolicy.shouldLaunchFreeform(true, true))
    }
}
