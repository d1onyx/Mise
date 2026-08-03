package com.dishlab.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object DatabaseFactory {
    fun create(
        url: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/dishlab",
        user: String = System.getenv("DATABASE_USER") ?: "dishlab",
        password: String = System.getenv("DATABASE_PASSWORD") ?: "dishlab",
    ): DataSource {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
        })
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        return dataSource
    }
}
