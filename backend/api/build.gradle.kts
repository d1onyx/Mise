plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("io.ktor.plugin") version "3.5.0"
}

ktor {
    openApi { }
}

// :api is a library module consumed by :app, not a runnable service — the
// io.ktor.plugin applies fat-jar/dist tasks unconditionally for the compiler
// plugin, but they need a main class this module doesn't have.
tasks.matching { it.name in setOf("shadowJar", "startScripts", "distTar", "distZip", "buildFatJar") }
    .configureEach { enabled = false }

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
    implementation(ktorLibs.server.routingOpenapi)
    implementation(ktorLibs.server.swagger)
    // Bundled swagger-ui static assets (webjars convention: META-INF/resources/webjars/swagger-ui/<version>/)
    // so /swagger renders without reaching unpkg.com — see OpenApiRoutes.kt.
    implementation("org.webjars:swagger-ui:5.32.13")
}
