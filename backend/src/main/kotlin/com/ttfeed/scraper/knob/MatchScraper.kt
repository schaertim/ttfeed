package com.ttfeed.scraper.knob

import com.ttfeed.database.*
import com.ttfeed.model.MatchStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

class MatchScraper(
    private val client: KnobClient,
    private val parser: KnobParser
) {
    private val logger = LoggerFactory.getLogger(MatchScraper::class.java)

    suspend fun run() {
        val matches = transaction {
            (Matches innerJoin Groups innerJoin Divisions innerJoin Federations innerJoin Seasons)
                .select(
                    Matches.id,
                    Matches.knobMatchId,
                    Matches.homeTeamId,
                    Matches.awayTeamId,
                    Groups.knobGruppe,
                    Federations.name,
                    Seasons.id,
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
                        homeTeamId  = it[Matches.homeTeamId],
                        awayTeamId  = it[Matches.awayTeamId],
                        knobGruppe  = it[Groups.knobGruppe],
                        rvid        = FEDERATION_RVIDS[it[Federations.name]],
                        seasonId    = it[Seasons.id],
                        season      = it[Seasons.name]
                    )
                }
        }

        logger.info("MatchScraper: ${matches.size} completed matches without game details")

        for ((index, match) in matches.withIndex()) {
            try {
                scrapeMatch(match)
                if (index % 100 == 0) {
                    logger.info("Progress: $index / ${matches.size} matches scraped")
                }
            } catch (e: Exception) {
                logger.error("Failed matchId=${match.knobMatchId} gruppe=${match.knobGruppe}: ${e.message}")
            }
        }

        logger.info("MatchScraper complete")
    }

    private suspend fun scrapeMatch(match: MatchToScrape) {
        val html   = client.fetchMatchDetail(match.knobGruppe, match.knobMatchId, match.season, match.rvid)
        val detail = parser.parseMatchDetail(html, match.knobMatchId)

        if (detail.games.isEmpty()) {
            logger.debug("No games found for matchId=${match.knobMatchId}")
            return
        }

        transaction {
            for (game in detail.games) {
                val homePlayer1Id = upsertPlayer(game.homePlayer1KnobId, game.homePlayer1Name, match.homeTeamId, match.seasonId)
                val homePlayer2Id = upsertPlayer(game.homePlayer2KnobId, game.homePlayer2Name, match.homeTeamId, match.seasonId)
                val awayPlayer1Id = upsertPlayer(game.awayPlayer1KnobId, game.awayPlayer1Name, match.awayTeamId, match.seasonId)
                val awayPlayer2Id = upsertPlayer(game.awayPlayer2KnobId, game.awayPlayer2Name, match.awayTeamId, match.seasonId)

                Games.insertIgnore {
                    it[Games.matchId]       = match.matchId
                    it[Games.gameType]      = game.gameType
                    it[Games.orderInMatch]  = game.orderInMatch.toShort()
                    it[Games.homePlayer1Id] = homePlayer1Id
                    it[Games.homePlayer2Id] = homePlayer2Id
                    it[Games.awayPlayer1Id] = awayPlayer1Id
                    it[Games.awayPlayer2Id] = awayPlayer2Id
                    it[Games.homeSets]      = game.homeSets?.toShort()
                    it[Games.awaySets]      = game.awaySets?.toShort()
                    it[Games.result]        = game.result
                }

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

    /**
     * Returns the player's UUID, inserting a new player and player_season record if the knobId
     * is not yet in the database. This ensures match detail scraping never produces null player
     * references — players who only appear as substitutes or guests are created on the fly.
     */
    private fun upsertPlayer(knobId: Int?, name: String?, teamId: UUID, seasonId: UUID): UUID? {
        knobId ?: return null

        val existing = Players.select(Players.id)
            .where { Players.knobId eq knobId }
            .firstOrNull()

        val playerId = if (existing != null) {
            existing[Players.id]
        } else {
            Players.insertIgnore {
                it[Players.knobId]    = knobId
                it[Players.fullName]  = name ?: "Unknown"
                it[Players.licenceNr] = "$PLACEHOLDER_LICENCE_PREFIX$knobId"
            }
            Players.select(Players.id)
                .where { Players.knobId eq knobId }
                .first()[Players.id]
        }

        PlayerSeasons.insertIgnore {
            it[PlayerSeasons.playerId] = playerId
            it[PlayerSeasons.teamId]   = teamId
            it[PlayerSeasons.seasonId] = seasonId
        }

        return playerId
    }

    private data class MatchToScrape(
        val matchId: UUID,
        val knobMatchId: Int,
        val homeTeamId: UUID,
        val awayTeamId: UUID,
        val knobGruppe: Int,
        val rvid: Int?,
        val seasonId: UUID,
        val season: String
    )
}
