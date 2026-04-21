package com.ttfeed.scraper.clicktt

import com.ttfeed.database.*
import com.ttfeed.model.MatchStatus
import com.ttfeed.scraper.clicktt.ClickTTGroupScraper.Companion.toChampionship
import com.ttfeed.scraper.knob.PLACEHOLDER_LICENCE_PREFIX
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

class ClickTTMatchScraper(
    private val client: ClickTTClient,
    private val parser: ClickTTParser
) {
    private val logger = LoggerFactory.getLogger(ClickTTMatchScraper::class.java)

    /**
     * Finds all completed click-tt matches that have no game rows yet and scrapes their details.
     */
    suspend fun run() {
        val matches = transaction {
            (Matches innerJoin Groups innerJoin Divisions innerJoin Federations innerJoin Seasons)
                .select(
                    Matches.id,
                    Matches.clickttMatchId,
                    Matches.homeTeamId,
                    Matches.awayTeamId,
                    Matches.playedAt,
                    Groups.clickttId,
                    Federations.name,
                    Seasons.name
                )
                .where {
                    (Matches.status         eq MatchStatus.COMPLETED) and
                    (Matches.clickttMatchId.isNotNull()) and
                    (Groups.clickttId.isNotNull()) and
                    (Matches.id notInSubQuery Games.select(Games.matchId).withDistinct())
                }
                .map {
                    MatchToScrape(
                        matchId        = it[Matches.id],
                        clickttMatchId = it[Matches.clickttMatchId]!!,
                        clickttGroupId = it[Groups.clickttId]!!,
                        homeTeamId     = it[Matches.homeTeamId],
                        awayTeamId     = it[Matches.awayTeamId],
                        playedAt       = it[Matches.playedAt],
                        federationName = it[Federations.name],
                        season         = it[Seasons.name]
                    )
                }
        }

        logger.info("ClickTTMatchScraper: ${matches.size} completed matches without game details")

        for ((index, match) in matches.withIndex()) {
            try {
                scrapeMatch(match)
                if (index % 50 == 0) {
                    logger.info("Progress: $index / ${matches.size} matches scraped")
                }
            } catch (e: Exception) {
                logger.error("Failed meetingId=${match.clickttMatchId}: ${e.message}")
            }
        }

        logger.info("ClickTTMatchScraper complete")
    }

    private suspend fun scrapeMatch(match: MatchToScrape) {
        val championship = toChampionship(match.federationName, match.season)
        val html   = client.fetchMatchDetail(match.clickttMatchId, championship, match.clickttGroupId)
        val detail = parser.parseClickTTMatchDetail(html, match.clickttMatchId)

        if (detail.games.isEmpty()) {
            logger.debug("No games found for meetingId=${match.clickttMatchId}")
            return
        }

        transaction {
            val seasonId = Seasons.select(Seasons.id)
                .where { Seasons.name eq match.season }
                .first()[Seasons.id]

            for (game in detail.games) {
                val homePlayer1Id = upsertPlayer(game.homePersonId, game.homeName, game.homeKlass, match.homeTeamId, seasonId)
                val homePlayer2Id = upsertPlayer(game.homePersonId2, game.homeName2, null, match.homeTeamId, seasonId)
                val awayPlayer1Id = upsertPlayer(game.awayPersonId, game.awayName, game.awayKlass, match.awayTeamId, seasonId)
                val awayPlayer2Id = upsertPlayer(game.awayPersonId2, game.awayName2, null, match.awayTeamId, seasonId)

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
                    it[Games.playedAt]      = match.playedAt
                }

                val gameId = Games.select(Games.id)
                    .where {
                        (Games.matchId      eq match.matchId) and
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

        logger.debug("Scraped ${detail.games.size} games for meetingId=${match.clickttMatchId}")
    }

    /**
     * Looks up a player by their click-tt person ID (primary) or name (fallback).
     * Creates the player and player_season record if they don't exist yet.
     * Names from click-tt are in "Lastname, Firstname" format — stored as-is since the
     * backfill job uses the same format when it links click-tt data to existing players.
     */
    private fun upsertPlayer(
        personId: Int?,
        name: String?,
        klass: String?,
        teamId: UUID,
        seasonId: UUID
    ): UUID? {
        if (personId == null) {
            // Doubles player with no personId — try name lookup as a best-effort fallback
            name ?: return null
            return Players.select(Players.id)
                .where { Players.fullName eq name }
                .firstOrNull()?.get(Players.id)
                ?.also { playerId ->
                    PlayerSeasons.insertIgnore {
                        it[PlayerSeasons.playerId] = playerId
                        it[PlayerSeasons.teamId]   = teamId
                        it[PlayerSeasons.seasonId] = seasonId
                        it[PlayerSeasons.klass]    = klass
                    }
                }
        }

        val existing = Players.select(Players.id)
            .where { Players.clickttId eq personId }
            .firstOrNull()

        val playerId = if (existing != null) {
            existing[Players.id]
        } else {
            // New player — create a placeholder licence until the backfill job links their STT number
            val placeholderLicence = "${PLACEHOLDER_LICENCE_PREFIX}ct$personId"
            Players.insertIgnore {
                it[Players.clickttId]  = personId
                it[Players.fullName]   = name ?: "Unknown"
                it[Players.licenceNr]  = placeholderLicence
            }
            Players.select(Players.id)
                .where { Players.clickttId eq personId }
                .first()[Players.id]
        }

        PlayerSeasons.insertIgnore {
            it[PlayerSeasons.playerId] = playerId
            it[PlayerSeasons.teamId]   = teamId
            it[PlayerSeasons.seasonId] = seasonId
            it[PlayerSeasons.klass]    = klass
        }

        return playerId
    }

    private data class MatchToScrape(
        val matchId: UUID,
        val clickttMatchId: Int,
        val clickttGroupId: Int,
        val homeTeamId: UUID,
        val awayTeamId: UUID,
        val playedAt: java.time.OffsetDateTime?,
        val federationName: String,
        val season: String
    )
}
