// Self-contained on purpose: no convention plugin, no version catalog.
// Copy this folder into a project, add `include(":navigation")`, and it builds.
// Requires the `core` template — see README.md.

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("dev.zacsweers.metro")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val coroutinesVersion = "1.11.0"
val navigationVersion = "2.9.2"

kotlin {
    android {
        namespace = "com.d1onyx.navigation"
        compileSdk = 37
        minSdk = 24
        withHostTestBuilder {}.configure {}
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()
    explicitApi()

    sourceSets {
        commonMain.dependencies {
            // Plain project() rather than the type-safe accessor, so the host
            // project does not have to enable TYPESAFE_PROJECT_ACCESSORS.
            api(project(":core"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)

            // Navigation 3 has no KMP port yet (androidx.navigation3 is
            // Android-only), so this is JetBrains' Navigation 2 build.
            api("org.jetbrains.androidx.navigation:navigation-compose:$navigationVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
        }
    }
}
