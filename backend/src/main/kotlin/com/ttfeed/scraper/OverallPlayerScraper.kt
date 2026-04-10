package com.ttfeed.scraper

import com.ttfeed.database.Players
import com.ttfeed.database.Seasons
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.*

class OverallPlayerScraper(
    private val client: KnobClient,
    private val parser: KnobParser
) {
    private val logger = LoggerFactory.getLogger(OverallPlayerScraper::class.java)

    suspend fun run() {
        val seasons = transaction {
            Seasons.select(Seasons.id, Seasons.name)
                .map { it[Seasons.id] to it[Seasons.name] }
        }

        logger.info("OverallPlayerScraper: ${seasons.size} seasons to scrape")

        // Build lookup once — covers all players regardless of season.
        // License lives on the player row, not on player_season, so no season scoping needed.
        // After the GroupScraper phase 1 fix, DB names are already clean (no age suffixes),
        // so a direct name lookup is sufficient.
        val existingPlayers: Map<String, Pair<String, UUID>> = transaction {
            Players.select(Players.id, Players.fullName, Players.licenceNr)
                .associate { row ->
                    row[Players.fullName] to Pair(row[Players.licenceNr], row[Players.id])
                }
        }

        var totalUpdated  = 0
        var totalInserted = 0

        for ((_, seasonName) in seasons) {
            try {
                val (updated, inserted) = scrapeSeason(seasonName, existingPlayers)
                totalUpdated  += updated
                totalInserted += inserted
            } catch (e: Exception) {
                logger.error("Failed season=$seasonName: ${e.message}")
            }
        }

        logger.info("OverallPlayerScraper complete — $totalUpdated updated, $totalInserted new inserts")
    }

    private suspend fun scrapeSeason(
        seasonName: String,
        existingPlayers: Map<String, Pair<String, UUID>>
    ): Pair<Int, Int> {
        val html    = client.fetchOverallPlayers(seasonName)
        val players = parser.parseOverallPlayers(html)

        if (players.isEmpty()) {
            logger.info("  $seasonName — no players found (licence registry predates online records)")
            return 0 to 0
        }

        logger.info("  $seasonName — ${players.size} licensed players scraped")

        var updated  = 0
        var inserted = 0

        transaction {
            for (player in players) {
                val match = existingPlayers[player.fullName]

                if (match != null) {
                    val (currentLicence, playerId) = match
                    // Only update if still a placeholder and the license isn't already taken
                    if (currentLicence.startsWith(PLACEHOLDER_LICENCE_PREFIX)) {
                        val licenceTaken = Players.select(Players.id)
                            .where { Players.licenceNr eq player.licenceNr }
                            .firstOrNull() != null

                        if (!licenceTaken) {
                            Players.update({ Players.id eq playerId }) {
                                it[Players.licenceNr] = player.licenceNr
                            }
                            updated++
                        }
                    }
                } else {
                    // Player appears in the license registry but not in any group ranking.
                    // Insert as a bare player record — no team association.
                    Players.insertIgnore {
                        it[Players.licenceNr] = player.licenceNr
                        it[Players.fullName]  = player.fullName
                    }
                    inserted++
                }
            }
        }

        if (updated > 0 || inserted > 0) {
            logger.info("    → $updated updated, $inserted new inserts")
        }

        return updated to inserted
    }
}