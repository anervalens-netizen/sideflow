# Build-tool dependency security

The Android buildscript selects patched libraries without changing application dependencies or the existing AGP/Kotlin/R8/SDK stack:

| Build-only library | Selected version |
| --- | --- |
| Protobuf Java | 3.25.9 |
| Netty, aligned through its BOM | 4.1.139.Final |
| Commons IO | 2.22.0 |
| Commons Compress | 1.28.0 |
| jose4j | 0.9.7 |
| JDOM | 2.0.6.1 |
| Bouncy Castle provider / PKIX / utility | 1.86, coherent `jdk18on` artifacts |

Protobuf stays on the existing major runtime line for the generated Android build models, following the [upstream compatibility guarantee](https://protobuf.dev/support/cross-version-runtime-guarantee/). The Netty BOM aligns all modules used by AGP's gRPC instrumentation transport. The Commons releases retain Java 8 support; the build uses Java 17. The [JDOM release](https://github.com/hunterhacker/jdom/releases/tag/JDOM-2.0.6.1) fixes the handling of an explicit external-entity-disable feature. JDOM still permits entity expansion by default; callers handling untrusted XML must disable it explicitly. Jetifier is disabled in this project.

Bouncy Castle's [upstream artifact policy](https://github.com/bcgit/bc-java#suffix-history) retires `jdk15on` and names `jdk18on` as the maintained Java 8+ family. Explicit buildscript dependency substitutions replace the old provider and PKIX artifacts, and provider/PKIX/transitive `bcutil` all resolve to 1.86. Old and new provider families must not coexist. This is a visible artifact migration, not a scan exclusion.

These libraries remain in the build/test classpath. The application source, runtime/test dependency graphs, application ID, SDK levels and version are unchanged. The owner-accepted installed APK remains code13 from `afff2d3276346a094dec15fb25b23eb64e659918`, as recorded in [the dated delivery](DELIVERY_20261015.md). No new installation or physical acceptance is implied.

## Kotlin's package-level advisory match

The required Kotlin Gradle plugin 1.9.24 still matches [GHSA-r937-wjx7-w2jp](https://github.com/advisories/GHSA-r937-wjx7-w2jp) by package/version. The [upstream correction](https://github.com/JetBrains/kotlin/commit/bf51df665b458fda7c3eaf436c4d88dc119d7ec6) changes KAPT incremental-cache deserialization. That implementation is absent from the actual resolved buildscript JARs: neither the historical `kapt3/base/incremental` nor current `kapt/base/incremental` packages, nor their affected cache-class basenames, are present. KAPT plugins and tasks are also absent.

The raw advisory match is retained. No advisory is suppressed and no all-zero package scan is claimed. The build now fails before task execution if either KAPT plugin ID is applied. Enabling KAPT requires a supported toolchain patched for that advisory; it must not silently invalidate this applicability evidence. The Android Kotlin compiler remains unchanged.

## Validation boundary

Qualification checks the complete resolved graph and actual classpath, existing unit/build/lint and Android instrumentation gates, malformed Protobuf rejection, and compatibility of AGP's certificate/keystore generation, JOSE signatures, archive/XML helpers (including JDOM's explicit external-entity-disable feature) and certificate-gated gRPC transport. Synthetic signing material belongs only to disposable fixtures.

AGP's instrumentation-result listener requires a generated client certificate; its default port binding is not a loopback-only guarantee. A loopback transport fixture does not change or directly probe that listener's binding. Do not expose test listeners. The accepted physical delivery and its unexecuted OEM/performance cases retain their dated scope.
