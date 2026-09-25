package eu.astancu.sideflow

import org.junit.Assert.*
import org.junit.Test

class RuntimeAndGeometryTest {
    @Test fun explicitStopBlocksEveryRecoveryPath() {
        assertFalse(RecoveryPolicy.sameBoot(false, 3, 3, true))
        assertFalse(RecoveryPolicy.newBoot(false, true, 4, 3, true))
    }
    @Test fun processDeathAndNewBootDiffer() {
        assertTrue(RecoveryPolicy.sameBoot(true, 3, 3, true))
        assertFalse(RecoveryPolicy.sameBoot(true, 4, 3, true))
        assertFalse(RecoveryPolicy.newBoot(true, false, 4, 3, true))
        assertTrue(RecoveryPolicy.newBoot(true, true, 4, 3, true))
        assertFalse(RecoveryPolicy.newBoot(true, true, 4, 3, false))
    }
    @Test fun geometryClampsExtremeViewportWithoutInvalidRange() {
        assertEquals(1 to 1, SidebarStyle.dimensions(0, 0, 180, 700))
        assertEquals(80 to 20, SidebarStyle.dimensions(80, 20, 180, 700))
        assertEquals(0, SidebarStyle.handleOffset(20, 115, 214))
        assertEquals(50, SidebarStyle.handleOffset(200, 100, 214))
        assertEquals(-50, SidebarStyle.handleOffset(200, 100, -214))
    }
    @Test fun slowIconSurvivesCloseReopenButNotPackageChange() {
        val policy = IconRequestPolicy()
        val old = policy.ticket("test.example.alpha")
        assertTrue(policy.current(old)) // close/reopen does not discard valid hidden work
        policy.invalidate("test.example.alpha")
        assertFalse(policy.current(old))
        val latest = policy.ticket("test.example.alpha")
        assertTrue(policy.current(latest))
        policy.newData()
        assertFalse(policy.current(latest))
        policy.destroy()
        assertFalse(policy.current(policy.ticket("test.example.alpha")))
    }
}
