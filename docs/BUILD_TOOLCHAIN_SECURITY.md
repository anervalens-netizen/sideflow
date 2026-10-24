# Build-tool dependency security

The Android buildscript pins Protobuf Java **3.25.9** and the coherent Netty **4.1.139.Final BOM**. Protobuf stays on the same major runtime line for the existing generated Android build models. The BOM aligns every Netty module used by AGP's gRPC instrumentation-result transport; individual modules are not independently mixed.

These are build/test dependencies. They do not enter the application's runtime dependency graph, and this change preserves the Android source, AGP/R8/Kotlin compiler, SDK levels, application ID and version. The owner-accepted installed APK remains the signed code13 build from `afff2d3276346a094dec15fb25b23eb64e659918`, as recorded in [the dated delivery](DELIVERY_20261015.md). It was not replaced by this build-tool update.

The Protobuf change addresses [GHSA-735f-pc8j-v9w8](https://github.com/protocolbuffers/protobuf/security/advisories/GHSA-735f-pc8j-v9w8). The existing generated code/newer same-major runtime pairing follows the [upstream compatibility guarantee](https://protobuf.dev/support/cross-version-runtime-guarantee/). Netty's BOM keeps its build transport on the patched 4.1 line while retaining the existing gRPC version.

## Scope of the retained tooling findings

The wider buildscript graph also contains library versions with advisory matches. Those matches are retained in the security evidence; this is not a claim of an all-zero dependency scan. In the current configuration:

- The Kotlin KAPT plugin, tasks and incremental-cache implementation are absent.
- Jetifier is disabled; its JDOM POM parser is not used.
- The affected Commons IO XML reader and Commons Compress Pack200/DUMP parsers have no external bytecode references in the resolved buildscript jars.
- Bundletool's external jose4j uses concern code-transparency signatures, with no external JWE decryption reference found.
- Bouncy Castle use is scoped to local signing inputs and generated instrumentation certificates. Malformed local certificate/PEM inputs remain a build-input trust boundary.

AGP's instrumentation-result listener requires a generated client certificate. Its default port binding is not a loopback-only guarantee. The local transport check binds its disposable fixture to loopback; this does not change AGP's listener configuration. Do not expose test listeners or treat certificate-gated test traffic as an application feature. Static reference checks support this scoped assessment; they are not a universal proof against reflective or future use. Reassess these findings before enabling dormant tooling or introducing untrusted build inputs.
