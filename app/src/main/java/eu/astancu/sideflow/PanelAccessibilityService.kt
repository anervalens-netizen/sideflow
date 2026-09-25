package eu.astancu.sideflow

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Existing component identity retained for OEM process recovery only. */
class PanelAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply { eventTypes = 0 }
        if (RuntimeState(this).recoverSameBoot()) ServiceControl.start(this)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
