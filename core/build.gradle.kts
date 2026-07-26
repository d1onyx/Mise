// Self-contained on purpose: no convention plugin, no version catalog.
// Copy this folder into a project, add `include(":core")`, and it builds.
// See README.md for the two lines the host project has to provide.

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("dev.zacsweers.metro")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val coroutinesVersion = "1.11.0"
val serializationVersion = "1.11.0"
val datetimeVersion = "0.8.0"
val lifecycleVersion = "2.11.0"
val ktorVersion = "3.5.1"
val dataStoreVersion = "1.2.1"
val okioVersion = "3.18.0"
val androidxCoreVersion = "1.18.0"

kotlin {
    android {
        namespace = "com.d1onyx.core"
        // API 37 ships only as minor releases (android-37.0) — there is no plain
        // `android-37` platform, so the minor level has to be explicit.
        compileSdk {
            version = release(37) { minorApiLevel = 0 }
        }
        minSdk = 24

        // Creates the `androidHostTest` source set. Without it there is no JVM
        // test target at all, and everything in commonTest silently never runs
        // on a machine where the iOS targets are unavailable.
        withHostTestBuilder {}.configure {}
    }

    // No iosX64: the Intel simulator is being dropped across the ecosystem —
    // androidx.lifecycle already publishes only ios_arm64 and ios_simulator_arm64.
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()
    explicitApi()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            api("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
            api("io.ktor:ktor-client-core:$ktorVersion")
            api("androidx.datastore:datastore-preferences:$dataStoreVersion")

            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-client-logging:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            implementation("com.squareup.okio:okio:$okioVersion")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            implementation("androidx.core:core-ktx:$androidxCoreVersion")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:$ktorVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
            implementation("io.ktor:ktor-client-mock:$ktorVersion")
            implementation("com.squareup.okio:okio-fakefilesystem:$okioVersion")
        }
    }
}
