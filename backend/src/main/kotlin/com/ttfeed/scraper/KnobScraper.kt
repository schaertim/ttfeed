package com.ttfeed.scraper

import com.ttfeed.database.*
import com.ttfeed.scraper.model.ParsedMatch
import com.ttfeed.scraper.model.ParsedMatchDetail
import com.ttfeed.scraper.model.ParsedPlayer
import com.ttfeed.scraper.model.ParsedTeam
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class KnobScraper(
    private val client: KnobClient,
    private val parser: KnobParser
) {
    private val logger = LoggerFactory.getLogger(KnobScraper::class.java)

    suspend fun scrapeDivision(gruppeId: Int, seasonName: String, leagueName: String, divisionName: String) {
        logger.info("Scraping division: $divisionName (gruppe=$gruppeId, season=$seasonName)")

        val html = client.fetchDivisionPage(gruppeId, seasonName)
        val page = parser.parseDivisionPage(html)

        transaction {
            val seasonId = upsertSeason(seasonName)
            val leagueId = upsertLeague(leagueName)
            val divisionId = upsertDivision(divisionName, leagueId, seasonId, gruppeId)
            val teamIdMap = upsertTeams(page.teams, divisionId)
            upsertPlayers(page.players, teamIdMap, seasonId)
            upsertMatches(page.matches, divisionId, teamIdMap)
        }

        logger.info("Division $divisionName done — ${page.teams.size} teams, ${page.players.size} players, ${page.matches.size} matches")
    }

    suspend fun scrapeMatchDetail(gruppeId: Int, knobMatchId: Int, season: String) {
        logger.info("Scraping match detail: matchid=$knobMatchId gruppe=$gruppeId season=$season")
        val html = client.fetchMatchDetail(gruppeId, knobMatchId, season)
        val detail = parser.parseMatchDetail(html, knobMatchId)

        transaction {
            val matchUuid = Matches
                .select(Matches.id)
                .where { Matches.knobMatchId eq knobMatchId }
                .firstOrNull()
                ?.get(Matches.id) ?: run {
                logger.warn("Match $knobMatchId not found in DB, skipping")
                return@transaction
            }

            upsertGames(detail, matchUuid)
        }
        logger.info("Match detail $knobMatchId done — ${detail.games.size} games")
    }

    // ---- Upsert helpers ----

    private fun upsertSeason(name: String): Int {
        Seasons.insertIgnore { it[Seasons.name] = name }
        return Seasons
            .select(Seasons.id)
            .where { Seasons.name eq name }
            .first()[Seasons.id]
    }

    private fun upsertLeague(name: String): Int {
        Leagues.insertIgnore { it[Leagues.name] = name }
        return Leagues
            .select(Leagues.id)
            .where { Leagues.name eq name }
            .first()[Leagues.id]
    }

    private fun upsertDivision(name: String, leagueId: Int, seasonId: Int, gruppeId: Int): Int {
        Divisions.insertIgnore {
            it[Divisions.name] = name
            it[Divisions.leagueId] = leagueId
            it[Divisions.seasonId] = seasonId
            it[Divisions.knobGruppe] = gruppeId
            it[Divisions.matchFormat] = 10
        }
        return Divisions
            .select(Divisions.id)
            .where {
                (Divisions.leagueId eq leagueId) and
                        (Divisions.seasonId eq seasonId) and
                        (Divisions.name eq name)
            }
            .first()[Divisions.id]
    }

    private fun upsertTeams(teams: List<ParsedTeam>, divisionId: Int): Map<Int, UUID> {
        val teamIdMap = mutableMapOf<Int, UUID>()

        for (team in teams) {
            Clubs.insertIgnore {
                it[Clubs.name] = team.name.substringBeforeLast(" ").trim()
                it[Clubs.knobId] = team.knobClubId
            }
            val clubId = Clubs
                .select(Clubs.id)
                .where { Clubs.knobId eq team.knobClubId }
                .first()[Clubs.id]

            Teams.insertIgnore {
                it[Teams.name] = team.name
                it[Teams.clubId] = clubId
                it[Teams.divisionId] = divisionId
                it[Teams.knobId] = team.knobTeamId
            }
            val teamId = Teams
                .select(Teams.id)
                .where { Teams.knobId eq team.knobTeamId }
                .first()[Teams.id]

            teamIdMap[team.knobTeamId] = teamId
        }

        return teamIdMap
    }

    private fun upsertPlayers(players: List<ParsedPlayer>, teamIdMap: Map<Int, UUID>, seasonId: Int) {
        for (player in players) {
            Players.insertIgnore {
                it[Players.fullName] = player.fullName
                it[Players.knobId] = player.knobId
                it[Players.licenceNr] = "knob:${player.knobId}"
            }
            val playerId = Players
                .select(Players.id)
                .where { Players.knobId eq player.knobId }
                .first()[Players.id]

            val teamId = teamIdMap[player.knobTeamId] ?: continue

            PlayerSeasons.insertIgnore {
                it[PlayerSeasons.playerId] = playerId
                it[PlayerSeasons.teamId] = teamId
                it[PlayerSeasons.seasonId] = seasonId
                it[PlayerSeasons.klass] = player.klass
            }
        }
    }

    private fun upsertMatches(matches: List<ParsedMatch>, divisionId: Int, teamIdMap: Map<Int, UUID>) {
        for (match in matches) {
            val homeTeamId = teamIdMap[match.homeKnobTeamId] ?: continue
            val awayTeamId = teamIdMap[match.awayKnobTeamId] ?: continue
            val playedAt = match.playedAt?.let { parseDate(it) }

            Matches.insertIgnore {
                it[Matches.knobMatchId] = match.knobMatchId
                it[Matches.divisionId] = divisionId
                it[Matches.homeTeamId] = homeTeamId
                it[Matches.awayTeamId] = awayTeamId
                it[Matches.round] = match.round
                it[Matches.playedAt] = playedAt
                it[Matches.homeScore] = match.homeScore?.toShort()
                it[Matches.awayScore] = match.awayScore?.toShort()
                it[Matches.status] = match.status
            }

            if (match.status == "completed") {
                Matches.update({ Matches.knobMatchId eq match.knobMatchId }) {
                    it[Matches.homeScore] = match.homeScore?.toShort()
                    it[Matches.awayScore] = match.awayScore?.toShort()
                    it[Matches.status] = match.status
                }
            }
        }
    }

    private fun upsertGames(detail: ParsedMatchDetail, matchId: UUID) {
        for (game in detail.games) {
            val homePlayerId = game.homePlayer1KnobId?.let { knobId ->
                Players.select(Players.id)
                    .where { Players.knobId eq knobId }
                    .firstOrNull()?.get(Players.id)
            }
            val awayPlayerId = game.awayPlayer1KnobId?.let { knobId ->
                Players.select(Players.id)
                    .where { Players.knobId eq knobId }
                    .firstOrNull()?.get(Players.id)
            }

            val existingGameId = Games
                .select(Games.id)
                .where {
                    (Games.matchId eq matchId) and
                            (Games.orderInMatch eq game.orderInMatch.toShort())
                }
                .firstOrNull()
                ?.get(Games.id)

            val gameId = if (existingGameId != null) {
                existingGameId
            } else {
                Games.insert {
                    it[Games.matchId] = matchId
                    it[Games.gameType] = game.gameType
                    it[Games.orderInMatch] = game.orderInMatch.toShort()
                    it[Games.homePlayer1Id] = homePlayerId
                    it[Games.awayPlayer1Id] = awayPlayerId
                    it[Games.homeSets] = game.homeSets?.toShort()
                    it[Games.awaySets] = game.awaySets?.toShort()
                    it[Games.result] = game.result
                }[Games.id]
            }

            for (set in game.sets) {
                GameSets.insertIgnore {
                    it[GameSets.gameId] = gameId
                    it[GameSets.setNumber] = set.setNumber.toShort()
                    it[GameSets.homePoints] = set.homePoints.toShort()
                    it[GameSets.awayPoints] = set.awayPoints.toShort()
                }
            }
        }
    }

    private fun parseDate(raw: String): OffsetDateTime? {
        return try {
            val withoutDay = raw.substringAfter(". ").trim()
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            val local = LocalDateTime.parse(withoutDay, formatter)
            local.atOffset(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.warn("Could not parse date: $raw")
            null
        }
    }
}