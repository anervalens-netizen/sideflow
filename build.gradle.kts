// Keep Android build/test transports patched without changing APK dependencies.
buildscript {
    val bouncyCastleVersion = "1.86"
    configurations.classpath {
        resolutionStrategy.dependencySubstitution {
            // Upstream retired jdk15on after 1.70; jdk18on is its maintained Java 8+ BC 1.x family.
            substitute(module("org.bouncycastle:bcprov-jdk15on"))
                .using(module("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion"))
                .because("Replace the EOL provider with its maintained, patched artifact family")
            substitute(module("org.bouncycastle:bcpkix-jdk15on"))
                .using(module("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion"))
                .because("Keep provider, PKIX and transitive bcutil on the same patched BC version")
        }
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-java:3.25.9") {
            because("GHSA-735f-pc8j-v9w8: bound recursion when parsing malformed build/test messages")
        }
        classpath("commons-io:commons-io:2.22.0") {
            because("Patch XmlStreamReader resource exhaustion while retaining the Commons IO 2.x API")
        }
        classpath("org.apache.commons:commons-compress:1.28.0") {
            because("Patch malformed Pack200/DUMP processing on the maintained Java 8-compatible 1.x line")
        }
        classpath("org.bitbucket.b_c:jose4j:0.9.7") {
            because("Patch JWE algorithm and malformed-content handling without changing bundletool")
        }
        classpath("org.jdom:jdom2:2.0.6.1") {
            because("Apply the upstream CVE-2021-33813 fix to Jetifier's retained JDOM dependency")
        }
        classpath("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
        // Align every AGP gRPC test-listener Netty module on one patched 4.1 version.
        classpath(platform("io.netty:netty-bom:4.1.139.Final"))
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// KGP 1.9.24 is retained for app compilation; the vulnerable KAPT cache implementation is absent.
// Fail before task execution if a later edit tries to activate KAPT without updating the toolchain.
allprojects {
    listOf("org.jetbrains.kotlin.kapt", "kotlin-kapt").forEach { kaptPlugin ->
        pluginManager.withPlugin(kaptPlugin) {
            error("KAPT is disabled: upgrade to a toolchain patched for GHSA-r937-wjx7-w2jp before enabling it")
        }
    }
}
