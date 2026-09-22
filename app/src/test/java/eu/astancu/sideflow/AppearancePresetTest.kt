package eu.astancu.sideflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePresetTest {

    @Test
    fun storageValuesRoundTrip() {
        AppearancePresetKey.entries.forEach { preset ->
            assertEquals(preset, AppearancePresetKey.fromStorage(preset.storageValue))
        }
        assertEquals(AppearancePresetKey.CUSTOM, AppearancePresetKey.fromStorage("unknown"))
        assertEquals(AppearancePresetKey.CUSTOM, AppearancePresetKey.fromStorage(null))
    }

    @Test
    fun smokeGlassIsBlurredDarkGlass() {
        val spec = AppearancePresetCatalog.spec(AppearancePresetKey.SMOKE_GLASS)!!
        assertTrue(spec.blurEnabled)
        assertTrue(spec.blurAmount > 0)
        assertEquals("#FF1C1E22", spec.backgroundColor)
        assertEquals(85, spec.opacityPercent)
    }

    @Test
    fun frostedLightHasLightSurface() {
        val spec = AppearancePresetCatalog.spec(AppearancePresetKey.FROSTED_LIGHT)!!
        assertTrue(spec.blurEnabled)
        assertEquals("#FFF4F6F8", spec.backgroundColor)
        assertEquals(93, spec.opacityPercent)
    }

    @Test
    fun amoledDisablesBlurAndUsesBlack() {
        val spec = AppearancePresetCatalog.spec(AppearancePresetKey.AMOLED)!!
        assertFalse(spec.blurEnabled)
        assertEquals(SideFlowPolicy.MIN_BLUR_AMOUNT, spec.blurAmount)
        assertEquals("#FF000000", spec.backgroundColor)
        assertEquals(100, spec.opacityPercent)
    }

    @Test
    fun materialYouIsDynamicSurfacePreset() {
        val spec = AppearancePresetCatalog.spec(AppearancePresetKey.MATERIAL_YOU)!!
        assertTrue(spec.usesMaterialYouSurface)
        assertTrue(spec.blurEnabled)
        assertEquals(90, spec.opacityPercent)
    }

    @Test
    fun legacyAppearanceImportClearsStaleNamedPreset() {
        assertTrue(AppearancePresetCatalog.shouldTreatLegacyImportAsCustom(false, true))
        assertFalse(AppearancePresetCatalog.shouldTreatLegacyImportAsCustom(true, true))
        assertFalse(AppearancePresetCatalog.shouldTreatLegacyImportAsCustom(false, false))
    }

    @Test
    fun customAndMaterialYouUseResolvedPickerAccent() {
        assertTrue(
            AppearancePresetCatalog.shouldUseResolvedPickerAccent(
                AppearancePresetKey.MATERIAL_YOU,
                useCustomAccent = false
            )
        )
        assertTrue(
            AppearancePresetCatalog.shouldUseResolvedPickerAccent(
                AppearancePresetKey.CUSTOM,
                useCustomAccent = false
            )
        )
        assertTrue(
            AppearancePresetCatalog.shouldUseResolvedPickerAccent(
                AppearancePresetKey.SMOKE_GLASS,
                useCustomAccent = true
            )
        )
        assertFalse(
            AppearancePresetCatalog.shouldUseResolvedPickerAccent(
                AppearancePresetKey.SMOKE_GLASS,
                useCustomAccent = false
            )
        )
    }

    @Test
    fun customHasNoForcedSpec() {
        assertNull(AppearancePresetCatalog.spec(AppearancePresetKey.CUSTOM))
    }
}
