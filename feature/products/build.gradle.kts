plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.d1onix.dishlab.feature.products"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        // Without this the module's strings never reach the APK.
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
            // Previews are Android-only source (src/androidMain/.../*Previews.android.kt),
            // so the annotation and the renderer are Android-only dependencies.
            // Without the renderer the preview panel has nothing to draw with.
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":navigation"))
            implementation(project(":design-system"))
            implementation(project(":domain"))
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    // Puts the renderer on the Android runtime classpath — the same line the
    // wizard adds to :shared, and what the preview panel actually loads.
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.resources {
    packageOfResClass = "com.d1onix.dishlab.feature.products.resources"
}
