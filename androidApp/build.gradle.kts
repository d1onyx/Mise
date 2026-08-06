import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val backendUrl = providers.gradleProperty("dishLabApiUrl")
    .orElse("http://10.0.2.2:8080/")
    .get()
val admobAppId = providers.gradleProperty("dishLabAdmobAppId")
    .orElse("ca-app-pub-3940256099942544~3347511713")
    .get()

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    // `setContent { }` compiles in this module, so the Compose runtime has to be
    // a direct dependency — :shared keeps its Compose deps internal.
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.d1onix.dishlab"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.android.compileSdkMinor.get().toInt()

    defaultConfig {
        applicationId = "com.d1onix.dishlab"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "DISH_LAB_API_URL", "\"$backendUrl\"")
        buildConfigField("String", "DISH_LAB_DEV_TOKEN", "\"\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "DISH_LAB_DEV_TOKEN", "\":dishlab-mobile\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // The `core` template ships AndroidNotificationController, which lint
        // sees on the classpath. DishLab never posts a notification and never
        // binds that controller, so declaring POST_NOTIFICATIONS would be asking
        // the user for a permission the app does not use.
        disable += "NotificationPermission"
    }
}
