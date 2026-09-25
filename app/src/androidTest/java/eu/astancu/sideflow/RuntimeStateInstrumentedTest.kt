package eu.astancu.sideflow

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeStateInstrumentedTest {
    @Test fun durableStopOverridesLegacyEnabledState() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("synthetic_runtime_$name", mode)
        }
        val legacy = isolated.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE)
        val minimal = isolated.getSharedPreferences("minimal_runtime", Context.MODE_PRIVATE)
        legacy.edit().clear().putBoolean("service_enabled", true).commit()
        minimal.edit().clear().commit()
        try {
            assertTrue(RuntimeState(isolated).enabled)
            assertTrue(RuntimeState(isolated).setEnabled(false))
            assertFalse(RuntimeState(isolated).enabled)
            assertFalse(RecoveryPolicy.sameBoot(RuntimeState(isolated).enabled, 2, 2, true))
            assertFalse(RecoveryPolicy.newBoot(RuntimeState(isolated).enabled, true, 3, 2, true))
            assertTrue(RuntimeState(isolated).setEnabled(true))
            assertTrue(RuntimeState(isolated).markBoot())
            assertEquals(RuntimeState(isolated).bootCount, RuntimeState(isolated).storedBoot)
        } finally {
            legacy.edit().clear().commit()
            minimal.edit().clear().commit()
        }
    }
}
