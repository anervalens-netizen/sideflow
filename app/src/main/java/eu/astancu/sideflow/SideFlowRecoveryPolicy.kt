package eu.astancu.sideflow

object SideFlowRecoveryPolicy {
    fun shouldRecover(
        autoStart: Boolean,
        serviceRunning: Boolean,
        overlayAllowed: Boolean
    ): Boolean = autoStart && !serviceRunning && overlayAllowed
}
