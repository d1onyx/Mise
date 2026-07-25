rootProject.name = "dishlab"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":androidApp")
include(":shared")

// Vendored from ~/work/templates — kept byte-for-byte so the next template
// update is a plain folder copy.
include(":core")
include(":navigation")

include(":design-system")
include(":domain")
include(":data")

include(":feature:home")
include(":feature:scanner")
include(":feature:products")
include(":feature:recipes")