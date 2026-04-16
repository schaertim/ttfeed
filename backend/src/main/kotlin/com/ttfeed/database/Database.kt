package com.ttfeed.database

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

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

/**
 * Runs a blocking Exposed transaction on the IO dispatcher.
 * Use this instead of `withContext(Dispatchers.IO) { transaction { ... } }` everywhere.
 */
suspend fun <T> dbQuery(block: Transaction.() -> T): T =
    withContext(Dispatchers.IO) { transaction { block() } }