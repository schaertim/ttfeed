package com.ttfeed

import com.ttfeed.database.configureDatabase
import com.ttfeed.scraper.BackfillScraper
import com.ttfeed.scraper.KnobClient
import com.ttfeed.scraper.KnobParser
import com.ttfeed.scraper.KnobScraper
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()

    launch {
        try {
            val client = KnobClient()
            val parser = KnobParser()
            val scraper = KnobScraper(client, parser)
            val backfill = BackfillScraper(client, parser, scraper)

            // Test with just current season first
            backfill.runSingleSeason("2025/2026")

            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}