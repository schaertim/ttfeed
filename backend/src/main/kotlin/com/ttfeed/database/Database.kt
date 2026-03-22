package com.ttfeed.database

import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    val config = environment.config.config("database")
    val url = config.property("url").getString()
    val user = config.property("user").getString()
    val password = config.property("password").getString()

    Flyway.configure()
        .dataSource(url, user, password)
        .load()
        .migrate()

    Database.connect(
        url = url,
        driver = config.property("driver").getString(),
        user = user,
        password = password
    )
}