package eu.astancu.sideflow

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SideFlowRecoveryPolicyTest {
    @Test
    fun recoversOnlyWhenRequestedOverlayAllowedAndServiceStopped() {
        assertTrue(SideFlowRecoveryPolicy.shouldRecover(true, false, true))
        assertFalse(SideFlowRecoveryPolicy.shouldRecover(false, false, true))
        assertFalse(SideFlowRecoveryPolicy.shouldRecover(true, true, true))
        assertFalse(SideFlowRecoveryPolicy.shouldRecover(true, false, false))
    }

    @Test
    fun bootRecoveryRunsOncePerBoot() {
        assertTrue(
            SideFlowRecoveryPolicy.shouldRecoverForBoot(
                autoStart = true,
                serviceRunning = false,
                overlayAllowed = true,
                currentBootCount = 42,
                handledBootCount = 41
            )
        )
        assertFalse(
            SideFlowRecoveryPolicy.shouldRecoverForBoot(
                autoStart = true,
                serviceRunning = false,
                overlayAllowed = true,
                currentBootCount = 42,
                handledBootCount = 42
            )
        )
        assertFalse(
            SideFlowRecoveryPolicy.shouldRecoverForBoot(
                autoStart = false,
                serviceRunning = false,
                overlayAllowed = true,
                currentBootCount = 42,
                handledBootCount = 41
            )
        )
    }
}
