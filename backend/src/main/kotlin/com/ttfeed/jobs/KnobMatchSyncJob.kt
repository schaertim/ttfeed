package com.ttfeed.jobs

import com.ttfeed.service.GameService
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object KnobMatchSyncJob {

    fun start(application: Application) {
        application.launch(Dispatchers.IO) {
            application.environment.log.info("KnobMatchSyncJob started. Polling every 30 minutes.")

            while (isActive) {
                try {
                    // Einfach abfragen, ohne Season ID
                    val pendingMatches = GameService.getPendingKnobMatches()

                    if (pendingMatches.isNotEmpty()) {
                        application.environment.log.info("Found ${pendingMatches.size} pending knob.ch matches. Starting scrape...")

                        for (matchId in pendingMatches) {
                            // TODO: Hier KnobScraper.scrapeMatchDetails(matchId) aufrufen

                            delay(1000L)
                        }
                    }
                } catch (e: Exception) {
                    application.environment.log.error("Error during KnobMatchSyncJob execution", e)
                }

                delay(30 * 60 * 1000L)
            }
        }
    }
}