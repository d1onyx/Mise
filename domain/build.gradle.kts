plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "com.d1onix.dishlab.domain"
        // API 37 ships only as minor releases (android-37.0, android-37.1) — there
        // is no plain `android-37` platform, so the minor level has to be explicit
        // or the IDE looks for a compile target that does not exist.
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Without this there is no JVM test target at all and commonTest
        // silently never runs.
        withHostTestBuilder {}.configure {}
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // AppScope, DispatcherProvider and the logging helpers the use cases
            // are bound with. No UI, no Ktor, no Compose in this module.
            api(project(":core"))
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
