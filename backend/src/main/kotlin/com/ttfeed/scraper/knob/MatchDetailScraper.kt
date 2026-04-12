package com.ttfeed.scraper.knob

import com.ttfeed.database.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

class MatchDetailScraper(
    private val client: KnobClient,
    private val parser: KnobParser
) {
    private val logger = LoggerFactory.getLogger(MatchDetailScraper::class.java)

    suspend fun run() {
        // Use a subquery to avoid materialising 100k+ UUIDs as query parameters,
        // which would exceed Postgres's 65535-parameter limit
        val matches = transaction {
            (Matches innerJoin Groups innerJoin Divisions innerJoin Federations innerJoin Seasons)
                .select(
                    Matches.id,
                    Matches.knobMatchId,
                    Groups.knobGruppe,
                    Federations.name,
                    Seasons.name
                )
                .where {
                    (Matches.status eq MatchStatus.COMPLETED) and
                            (Matches.knobMatchId.isNotNull()) and
                            (Matches.id notInSubQuery Games.select(Games.matchId).withDistinct())
                }
                .map {
                    MatchToScrape(
                        matchId     = it[Matches.id],
                        knobMatchId = it[Matches.knobMatchId]!!,
                        knobGruppe  = it[Groups.knobGruppe],
                        rvid        = FEDERATION_RVIDS[it[Federations.name]],
                        season      = it[Seasons.name]
                    )
                }
        }

        logger.info("MatchDetailScraper: ${matches.size} completed matches without game details")

        for ((index, match) in matches.withIndex()) {
            try {
                scrapeMatchDetail(match)
                if (index % 100 == 0) {
                    logger.info("Progress: $index / ${matches.size} matches scraped")
                }
            } catch (e: Exception) {
                logger.error("Failed matchId=${match.knobMatchId} gruppe=${match.knobGruppe}: ${e.message}")
            }
        }

        logger.info("MatchDetailScraper complete")
    }

    private suspend fun scrapeMatchDetail(match: MatchToScrape) {
        val html   = client.fetchMatchDetail(match.knobGruppe, match.knobMatchId, match.season, match.rvid)
        val detail = parser.parseMatchDetail(html, match.knobMatchId)

        if (detail.games.isEmpty()) {
            logger.debug("No games found for matchId=${match.knobMatchId}")
            return
        }

        transaction {
            for (game in detail.games) {
                val homePlayerId = resolvePlayerId(game.homePlayer1KnobId)
                val awayPlayerId = resolvePlayerId(game.awayPlayer1KnobId)

                Games.insertIgnore {
                    it[Games.matchId]       = match.matchId
                    it[Games.gameType]      = game.gameType
                    it[Games.orderInMatch]  = game.orderInMatch.toShort()
                    it[Games.homePlayer1Id] = homePlayerId
                    it[Games.awayPlayer1Id] = awayPlayerId
                    it[Games.homeSets]      = game.homeSets?.toShort()
                    it[Games.awaySets]      = game.awaySets?.toShort()
                    it[Games.result]        = game.result
                }

                // Retrieve the inserted (or existing) game ID to attach set scores
                val gameId = Games.select(Games.id)
                    .where {
                        (Games.matchId eq match.matchId) and
                                (Games.orderInMatch eq game.orderInMatch.toShort())
                    }
                    .firstOrNull()?.get(Games.id) ?: continue

                for (set in game.sets) {
                    GameSets.insertIgnore {
                        it[GameSets.gameId]     = gameId
                        it[GameSets.setNumber]  = set.setNumber.toShort()
                        it[GameSets.homePoints] = set.homePoints.toShort()
                        it[GameSets.awayPoints] = set.awayPoints.toShort()
                    }
                }
            }
        }

        logger.debug("Scraped ${detail.games.size} games for matchId=${match.knobMatchId}")
    }

    private fun resolvePlayerId(knobId: Int?): UUID? {
        knobId ?: return null
        return Players.select(Players.id)
            .where { Players.knobId eq knobId }
            .firstOrNull()?.get(Players.id)
    }

    private data class MatchToScrape(
        val matchId: UUID,
        val knobMatchId: Int,
        val knobGruppe: Int,
        val rvid: Int?,
        val season: String
    )
}