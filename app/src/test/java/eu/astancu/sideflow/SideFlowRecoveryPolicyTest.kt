package eu.astancu.sideflow

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SideFlowRecoveryPolicyTest {
    @Test
    fun recoversOnlyWhenAutostartEnabledOverlayAllowedAndServiceStopped() {
        assertTrue(SideFlowRecoveryPolicy.shouldRecover(true, false, true))
        assertFalse(SideFlowRecoveryPolicy.shouldRecover(false, false, true))
        assertFalse(SideFlowRecoveryPolicy.shouldRecover(true, true, true))
        assertFalse(SideFlowRecoveryPolicy.shouldRecover(true, false, false))
    }
}
