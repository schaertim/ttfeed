package com.ttfeed

import com.ttfeed.database.configureDatabase
import com.ttfeed.plugins.configureCors
import com.ttfeed.plugins.configureRouting
import com.ttfeed.plugins.configureSerialization
import com.ttfeed.scraper.clicktt.ClickTTSeasonScraper
import com.ttfeed.scraper.clicktt.ClickTTSyncService
import com.ttfeed.scraper.knob.BackfillScraper
import com.ttfeed.service.SeasonService
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
        // Full overnight backfill — knob (all seasons) → click-tt (current season) → portraits
        try {
            environment.log.info("Backfill: starting knob.ch full historical scrape")
            BackfillScraper.create().run()
            environment.log.info("Backfill: knob.ch complete — starting click-tt season scrape")
            ClickTTSeasonScraper.create().run()
            environment.log.info("Backfill: click-tt complete — starting portrait/ELO backfill")
            val currentSeasonId = SeasonService.getCurrentSeasonId()
            if (currentSeasonId != null) {
                ClickTTSyncService.runPortraitBackfill(currentSeasonId)
            }
            environment.log.info("Backfill: complete")
        } catch (e: Exception) {
            environment.log.error("Backfill failed", e)
        }
    }
}
