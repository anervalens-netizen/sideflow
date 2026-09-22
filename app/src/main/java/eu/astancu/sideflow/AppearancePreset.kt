package eu.astancu.sideflow

enum class AppearancePresetKey(val storageValue: String, val displayName: String) {
    CUSTOM("custom", "Custom"),
    SMOKE_GLASS("smoke_glass", "Smoke Glass"),
    FROSTED_LIGHT("frosted_light", "Frosted Light"),
    AMOLED("amoled", "AMOLED"),
    MATERIAL_YOU("material_you", "Material You");

    companion object {
        fun fromStorage(value: String?): AppearancePresetKey =
            entries.firstOrNull { it.storageValue == value } ?: CUSTOM
    }
}

data class AppearancePresetSpec(
    val key: AppearancePresetKey,
    val backgroundColor: String,
    val opacityPercent: Int,
    val blurEnabled: Boolean,
    val blurAmount: Int,
    val cornerRadiusDp: Int,
    val usesMaterialYouSurface: Boolean = false
)

object AppearancePresetCatalog {
    val smokeGlass = AppearancePresetSpec(
        key = AppearancePresetKey.SMOKE_GLASS,
        backgroundColor = "#FF1C1E22",
        opacityPercent = 85,
        blurEnabled = true,
        blurAmount = 28,
        cornerRadiusDp = 28
    )

    val frostedLight = AppearancePresetSpec(
        key = AppearancePresetKey.FROSTED_LIGHT,
        backgroundColor = "#FFF4F6F8",
        opacityPercent = 93,
        blurEnabled = true,
        blurAmount = 32,
        cornerRadiusDp = 28
    )

    val amoled = AppearancePresetSpec(
        key = AppearancePresetKey.AMOLED,
        backgroundColor = "#FF000000",
        opacityPercent = 100,
        blurEnabled = false,
        blurAmount = SideFlowPolicy.MIN_BLUR_AMOUNT,
        cornerRadiusDp = 24
    )

    val materialYou = AppearancePresetSpec(
        key = AppearancePresetKey.MATERIAL_YOU,
        backgroundColor = "#FF1A1C1E",
        opacityPercent = 90,
        blurEnabled = true,
        blurAmount = 20,
        cornerRadiusDp = 28,
        usesMaterialYouSurface = true
    )

    fun spec(key: AppearancePresetKey): AppearancePresetSpec? = when (key) {
        AppearancePresetKey.CUSTOM -> null
        AppearancePresetKey.SMOKE_GLASS -> smokeGlass
        AppearancePresetKey.FROSTED_LIGHT -> frostedLight
        AppearancePresetKey.AMOLED -> amoled
        AppearancePresetKey.MATERIAL_YOU -> materialYou
    }

    fun shouldTreatLegacyImportAsCustom(
        hasPresetKey: Boolean,
        importsAppearanceValues: Boolean
    ): Boolean = !hasPresetKey && importsAppearanceValues

    fun canEditAccent(
        uiTheme: String,
        useCustomAccent: Boolean
    ): Boolean =
        uiTheme != PanelPreferences.THEME_ORIGIN || useCustomAccent

    fun shouldUseResolvedPickerAccent(
        preset: AppearancePresetKey,
        useCustomAccent: Boolean
    ): Boolean =
        preset == AppearancePresetKey.MATERIAL_YOU ||
            preset == AppearancePresetKey.CUSTOM ||
            useCustomAccent
}
