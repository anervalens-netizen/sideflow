// Keep Android build/test transports patched without changing APK dependencies.
buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-java:3.25.9") {
            because("GHSA-735f-pc8j-v9w8: bound recursion when parsing malformed build/test messages")
        }
        // Align every AGP gRPC test-listener Netty module on one patched 4.1 version.
        classpath(platform("io.netty:netty-bom:4.1.139.Final"))
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
