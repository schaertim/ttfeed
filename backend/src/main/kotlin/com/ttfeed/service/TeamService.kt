package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import com.ttfeed.model.MatchResponse
import com.ttfeed.model.TeamPlayerResponse
import com.ttfeed.model.TeamSummaryResponse
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.*
import java.util.*

object TeamService {
    private val homeTeam = Teams.alias("home_team")
    private val awayTeam = Teams.alias("away_team")

    suspend fun getTeamSummary(teamId: String): TeamSummaryResponse? {
        val uuid = teamId.toUuidOrNull() ?: return null
        return dbQuery {
            val teamRow = Teams.selectAll().where { Teams.id eq uuid }.singleOrNull()
                ?: return@dbQuery null

            val standing = Standings.selectAll().where { Standings.teamId eq uuid }.singleOrNull()
            val record   = "${standing?.get(Standings.won) ?: 0}-${standing?.get(Standings.drawn) ?: 0}-${standing?.get(Standings.lost) ?: 0}"
            val points   = standing?.get(Standings.points)?.toInt() ?: 0

            val matchHistory = Matches.selectAll()
                .where { (Matches.homeTeamId eq uuid) or (Matches.awayTeamId eq uuid) }
                .orderBy(Matches.playedAt to SortOrder.DESC)
                .limit(5)
                .map { it.toMatchResult(uuid) }

            TeamSummaryResponse(
                id      = teamRow[Teams.id].toString(),
                name    = teamRow[Teams.name],
                record  = record,
                points  = points,
                streak  = calculateStreak(matchHistory)
            )
        }
    }

    suspend fun getTeamRoster(teamId: String): List<TeamPlayerResponse>? {
        val uuid = teamId.toUuidOrNull() ?: return null
        return dbQuery {
            if (Teams.selectAll().where { Teams.id eq uuid }.empty()) return@dbQuery null

            val playerRows = (Players innerJoin PlayerSeasons)
                .selectAll()
                .where { PlayerSeasons.teamId eq uuid }
                .toList()

            if (playerRows.isEmpty()) return@dbQuery emptyList()

            val playerIds = playerRows.map { it[Players.id] }.toSet()

            // Fetch match IDs for this team in one query
            val teamMatchIds = Matches
                .select(Matches.id)
                .where { (Matches.homeTeamId eq uuid) or (Matches.awayTeamId eq uuid) }
                .map { it[Matches.id] }
                .toSet()

            // Fetch all relevant singles games within those matches in one query
            val wins   = mutableMapOf<UUID, Int>()
            val losses = mutableMapOf<UUID, Int>()

            if (teamMatchIds.isNotEmpty()) {
                Games
                    .select(Games.homePlayer1Id, Games.awayPlayer1Id, Games.result)
                    .where {
                        (Games.matchId inList teamMatchIds) and
                        (Games.gameType eq GameType.SINGLES) and
                        ((Games.homePlayer1Id inList playerIds) or (Games.awayPlayer1Id inList playerIds))
                    }
                    .forEach { row ->
                        val homeId = row[Games.homePlayer1Id]
                        val awayId = row[Games.awayPlayer1Id]
                        when (row[Games.result]) {
                            GameResult.HOME -> {
                                if (homeId in playerIds) wins[homeId!!]   = (wins[homeId]   ?: 0) + 1
                                if (awayId in playerIds) losses[awayId!!] = (losses[awayId] ?: 0) + 1
                            }
                            GameResult.AWAY -> {
                                if (awayId in playerIds) wins[awayId!!]   = (wins[awayId]   ?: 0) + 1
                                if (homeId in playerIds) losses[homeId!!] = (losses[homeId] ?: 0) + 1
                            }
                            else -> Unit
                        }
                    }
            }

            playerRows.map { playerRow ->
                val pId = playerRow[Players.id]
                TeamPlayerResponse(
                    id        = pId.toString(),
                    fullName  = playerRow[Players.fullName],
                    licenceNr = playerRow[Players.licenceNr],
                    wins      = wins[pId]   ?: 0,
                    losses    = losses[pId] ?: 0
                )
            }
        }
    }

    suspend fun getTeamMatches(teamId: String): List<MatchResponse>? {
        val uuid = teamId.toUuidOrNull() ?: return null
        return dbQuery {
            if (Teams.selectAll().where { Teams.id eq uuid }.empty()) return@dbQuery null

            Matches
                .join(homeTeam, JoinType.INNER, Matches.homeTeamId, homeTeam[Teams.id])
                .join(awayTeam, JoinType.INNER, Matches.awayTeamId, awayTeam[Teams.id])
                .selectAll()
                .where { (Matches.homeTeamId eq uuid) or (Matches.awayTeamId eq uuid) }
                .orderBy(Matches.playedAt to SortOrder.ASC)
                .map { it.toMatchResponse() }
        }
    }

    private fun calculateStreak(results: List<String>): String {
        if (results.isEmpty()) return "-"
        val first = results.first()
        val count = results.takeWhile { it == first }.size
        return "$count$first"
    }

    private fun ResultRow.toMatchResult(teamId: UUID): String {
        val home = this[Matches.homeScore] ?: 0
        val away = this[Matches.awayScore] ?: 0
        return if (this[Matches.homeTeamId] == teamId) {
            if (home > away) "W" else if (home < away) "L" else "D"
        } else {
            if (away > home) "W" else if (away < home) "L" else "D"
        }
    }

    private fun ResultRow.toMatchResponse() = MatchResponse(
        id        = this[Matches.id].toString(),
        homeTeam  = this[homeTeam[Teams.name]],
        awayTeam  = this[awayTeam[Teams.name]],
        homeScore = this[Matches.homeScore]?.toInt(),
        awayScore = this[Matches.awayScore]?.toInt(),
        round     = this[Matches.round],
        playedAt  = this[Matches.playedAt]?.toString(),
        status    = this[Matches.status]
    )
}
