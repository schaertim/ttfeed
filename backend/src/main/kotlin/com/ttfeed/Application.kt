package com.ttfeed

import com.ttfeed.database.configureDatabase
import com.ttfeed.plugins.configureCors
import com.ttfeed.plugins.configureRouting
import com.ttfeed.plugins.configureSerialization
import com.ttfeed.scraper.clicktt.ClickTTSeasonScraper
import com.ttfeed.scraper.knob.BackfillScraper
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
    configureCors()

    launch(Dispatchers.IO) {
        BackfillScraper.create().runForSeason("2024/2025")
        ClickTTSeasonScraper.create().run("2025/2026", federations = listOf("MTTV"))
    }
}