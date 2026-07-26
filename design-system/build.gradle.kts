plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.d1onix.dishlab.designsystem"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
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
        androidMain.dependencies {
            // Previews live in androidMain only, so the annotation and the
            // renderer are Android-only dependencies. `api` because the
            // multipreview annotations in preview/ are public and every feature
            // module annotates its own previews with them.
            api(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    // Puts the renderer on the Android runtime classpath — the same line the
    // wizard adds to :shared, and what the preview panel actually loads.
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.resources {
    packageOfResClass = "com.d1onix.dishlab.designsystem.resources"
}
