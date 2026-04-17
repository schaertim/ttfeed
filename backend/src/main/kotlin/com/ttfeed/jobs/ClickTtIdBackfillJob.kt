package com.ttfeed.jobs

import com.ttfeed.database.Clubs
import com.ttfeed.database.dbQuery
import com.ttfeed.scraper.clicktt.ClickTTClient
import com.ttfeed.scraper.clicktt.ClickTTParser
import com.ttfeed.service.PlayerService
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.update

object ClickTtIdBackfillJob {

    fun start(application: Application) {
        application.launch(Dispatchers.IO) {
            val log    = application.environment.log
            val client = ClickTTClient()
            val parser = ClickTTParser()

            var totalPlayers = 0
            var totalClubs   = 0
            var emptyPages   = 0

            // Swiss club IDs on click-tt are in this range — generous bounds to avoid missing any
            val clubIdRange = 32980..33290

            log.info("ClickTtIdBackfillJob: scanning ${clubIdRange.count()} club IDs")

            try {
                for (clickttClubId in clubIdRange) {
                    try {
                        val html = client.fetchClubMembersPage(clickttClubId)

                        if (!html.contains("Lizenzierte Spieler")) {
                            emptyPages++
                            if (emptyPages % 100 == 0) {
                                log.info("  Scanned up to club ID $clickttClubId — $emptyPages empty so far")
                            }
                            continue
                        }

                        val page = parser.parseClubPage(html)
                        if (page.members.isEmpty()) continue

                        emptyPages = 0

                        // Update player click-tt IDs and names from the authoritative click-tt source
                        val playerUpdates = page.members.associate { it.licence to Pair(it.personId, it.fullName) }
                        PlayerService.updateClickTtDataBatch(playerUpdates)
                        totalPlayers += page.members.size

                        // Match the click-tt club to our DB club by finding which club the majority
                        // of these licensed players already belong to (via their knob-scraped records)
                        val licences  = page.members.map { it.licence }
                        val ourClubId = PlayerService.findClubIdByLicences(licences)

                        if (ourClubId != null && page.clubName != null) {
                            dbQuery {
                                Clubs.update({ Clubs.id eq ourClubId }) {
                                    it[Clubs.clickttId] = clickttClubId
                                    it[Clubs.name]      = page.clubName
                                }
                            }
                            totalClubs++
                            log.info("  Club ID $clickttClubId → '${page.clubName}' — ${page.members.size} players mapped")
                        } else {
                            log.debug("  Club ID $clickttClubId (${page.clubName}) — no matching club found in DB")
                        }

                    } catch (e: Exception) {
                        application.environment.log.error("  Error fetching club ID $clickttClubId", e)
                    }

                    delay(1000L)
                }
            } finally {
                client.close()
            }

            log.info("ClickTtIdBackfillJob complete — $totalPlayers players and $totalClubs clubs linked")
        }
    }
}
