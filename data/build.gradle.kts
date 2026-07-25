plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    // Only for `composeResources/files/*.json` — the demo dataset lives in
    // resources rather than in Kotlin, so replacing it with a real API touches
    // this module only. No UI code here.
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.d1onix.dishlab.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        // Without this the module's composeResources never reach the APK.
        androidResources {
            enable = true
        }
        withHostTestBuilder {}.configure {}
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            api(project(":domain"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.components.resources)
            // The generated `Res` accessors are @Composable-aware, so the
            // runtime has to be present even though this module has no UI.
            implementation(libs.compose.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.d1onix.dishlab.data.resources"
}
