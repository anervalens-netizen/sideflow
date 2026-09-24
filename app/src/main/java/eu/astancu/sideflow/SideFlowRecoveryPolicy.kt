package eu.astancu.sideflow

object SideFlowRecoveryPolicy {
    fun shouldRecover(
        requested: Boolean,
        serviceRunning: Boolean,
        overlayAllowed: Boolean
    ): Boolean = requested && !serviceRunning && overlayAllowed

    fun shouldRecoverForBoot(
        autoStart: Boolean,
        serviceRunning: Boolean,
        overlayAllowed: Boolean,
        currentBootCount: Int,
        handledBootCount: Int
    ): Boolean =
        autoStart &&
            currentBootCount >= 0 &&
            currentBootCount != handledBootCount &&
            !serviceRunning &&
            overlayAllowed
}
