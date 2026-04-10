package com.ttfeed

import com.ttfeed.database.configureDatabase
import com.ttfeed.plugins.configureRouting
import com.ttfeed.plugins.configureSerialization
import com.ttfeed.scraper.BackfillScraper
import com.ttfeed.scraper.KnobClient
import com.ttfeed.scraper.KnobParser
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
    launch {
        val client = KnobClient()
        try {
            val backfill = BackfillScraper(client, KnobParser())
            backfill.run()
        } catch (e: Exception) {
            log.error("Backfill failed", e)
        } finally {
            client.close()
        }
    }
}