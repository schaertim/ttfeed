package com.ttfeed.jobs

import com.ttfeed.scraper.clicktt.ClickTTClient
import com.ttfeed.scraper.clicktt.ClickTTParser
import com.ttfeed.service.PlayerService
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ClickTtIdBackfillJob {

    fun start(application: Application) {
        application.launch(Dispatchers.IO) {
            application.environment.log.info("ClickTtIdBackfillJob: Starting brute-force club crawl...")

            val client = ClickTTClient()
            val parser = ClickTTParser()

            var totalPlayersFound = 0
            var emptyPagesCount = 0

            // Die Club IDs der Schweiz liegen ca. zwischen 32500 und 33500.
            // Wir machen den Puffer etwas großzügiger (2000 Requests sind in 30 Min durch).
            val startId = 32980
            val endId = 33290

            try {
                for (clubId in startId..endId) {
                    try {
                        val html = client.fetchClubMembersPage(clubId)

                        // Wenn die Seite kein "Lizenzierte Spieler" enthält, ist die ID ungültig/leer
                        if (!html.contains("Lizenzierte Spieler")) {
                            emptyPagesCount++
                            // Kleiner Status-Log alle 100 leeren Seiten
                            if (emptyPagesCount % 100 == 0) {
                                application.environment.log.info("Scanned up to Club ID $clubId... ($emptyPagesCount empty pages so far)")
                            }
                            continue
                        }

                        val mappings = parser.parseClubMembersToMappings(html)

                        if (mappings.isNotEmpty()) {
                            // Updatet alle Spieler dieses Clubs in deiner DB!
                            PlayerService.updateClickTtIdsBatch(mappings)
                            totalPlayersFound += mappings.size
                            application.environment.log.info("Club ID $clubId: Found and mapped ${mappings.size} players! (Total: $totalPlayersFound)")
                            emptyPagesCount = 0 // Reset counter
                        }
                    } catch (e: Exception) {
                        application.environment.log.error("Error fetching Club ID $clubId", e)
                    }

                    // 1 Sekunde Pause, um die Server zu schonen
                    delay(1000L)
                }
            } finally {
                client.close()
            }

            application.environment.log.info("ClickTtIdBackfillJob COMPLETE! Successfully mapped $totalPlayersFound players.")
        }
    }
}