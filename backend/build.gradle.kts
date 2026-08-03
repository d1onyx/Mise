buildscript {
    dependencies {
        // Flyway 10+ moved per-database support into separate artifacts; the Gradle
        // task resolves the JDBC driver and the Postgres plugin from the buildscript classpath.
        classpath("org.postgresql:postgresql:42.7.11")
        classpath("org.flywaydb:flyway-database-postgresql:11.18.0")
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.flyway)
}

group = "com.dishlab"
version = "1.0.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}

flyway {
    url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/dishlab"
    user = System.getenv("DATABASE_USER") ?: "dishlab"
    password = System.getenv("DATABASE_PASSWORD") ?: "dishlab"
    locations = arrayOf("filesystem:migrations/src/main/resources/db/migration")
}
