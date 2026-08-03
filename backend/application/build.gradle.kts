plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {

//    implementation(libs.firebase.admin)


    implementation(project(":domain"))
    implementation(ktorLibs.serialization.kotlinx.json)

    testImplementation(kotlin("test"))
}
