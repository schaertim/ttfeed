package com.ttfeed.jobs

import com.ttfeed.database.Clubs
import com.ttfeed.database.dbQuery
import com.ttfeed.scraper.clicktt.ClickTTClient
import com.ttfeed.scraper.clicktt.ClickTTParser
import com.ttfeed.service.PlayerService
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

class ClickTtIdBackfillJob(
    private val client: ClickTTClient,
    private val parser: ClickTTParser
) {
    private val logger = LoggerFactory.getLogger(ClickTtIdBackfillJob::class.java)

    suspend fun run() {
        var totalPlayers = 0
        var totalClubs   = 0
        var emptyPages   = 0

        // Swiss club IDs on click-tt are in this range — generous bounds to avoid missing any
        val clubIdRange = 32980..33290

        logger.info("ClickTtIdBackfillJob: scanning ${clubIdRange.count()} club IDs")

        for (clickttClubId in clubIdRange) {
            try {
                val html = client.fetchClubMembersPage(clickttClubId)

                if (!html.contains("Lizenzierte Spieler")) {
                    emptyPages++
                    if (emptyPages % 100 == 0) {
                        logger.info("  Scanned up to club ID $clickttClubId — $emptyPages empty so far")
                    }
                    continue
                }

                val page = parser.parseClubPage(html)
                if (page.members.isEmpty()) continue

                emptyPages = 0

                val playerUpdates = page.members.associate { it.licence to Pair(it.personId, it.fullName) }
                PlayerService.updateClickTtDataBatch(playerUpdates)
                totalPlayers += page.members.size

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
                    logger.info("  Club ID $clickttClubId → '${page.clubName}' — ${page.members.size} players mapped")
                } else {
                    logger.debug("  Club ID $clickttClubId (${page.clubName}) — no matching club found in DB")
                }

            } catch (e: Exception) {
                logger.error("  Error fetching club ID $clickttClubId", e)
            }

            delay(1000L)
        }

        logger.info("ClickTtIdBackfillJob complete — $totalPlayers players and $totalClubs clubs linked")
    }
}
