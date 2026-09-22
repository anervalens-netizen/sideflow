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
