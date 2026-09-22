# Add project specific ProGuard rules here.
# Keep app classes
-keep class eu.astancu.sideflow.** { *; }
-keepclassmembers class eu.astancu.sideflow.** { *; }

# Keep ViewBinding
-keep class eu.astancu.sideflow.databinding.** { *; }

# Skydoves ColorPickerView
-keep class com.skydoves.colorpickerview.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.GeneratedAppGlideModule
-keepclassmembers public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
