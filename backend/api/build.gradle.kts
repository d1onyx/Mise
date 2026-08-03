plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
}
