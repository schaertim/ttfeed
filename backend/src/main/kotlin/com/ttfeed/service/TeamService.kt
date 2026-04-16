package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.GameResult
import com.ttfeed.model.MatchResponse
import com.ttfeed.model.TeamPlayerResponse
import com.ttfeed.model.TeamSummaryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object TeamService {
    private val homeTeam = Teams.alias("home_team")
    private val awayTeam = Teams.alias("away_team")

    suspend fun getTeamSummary(teamId: String): TeamSummaryResponse? {
        val uuid = runCatching { UUID.fromString(teamId) }.getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            transaction {
                val teamRow = Teams.selectAll().where { Teams.id eq uuid }.singleOrNull() ?: return@transaction null

                val standing = Standings.selectAll().where { Standings.teamId eq uuid }.singleOrNull()
                val record = "${standing?.get(Standings.won) ?: 0}-${standing?.get(Standings.drawn) ?: 0}-${standing?.get(Standings.lost) ?: 0}"
                val points = standing?.get(Standings.points)?.toInt() ?: 0

                val matchHistory = Matches.selectAll()
                    .where { (Matches.homeTeamId eq uuid) or (Matches.awayTeamId eq uuid) }
                    .orderBy(Matches.playedAt to SortOrder.DESC)
                    .limit(5)
                    .map { it.toMatchResult(uuid) }

                TeamSummaryResponse(
                    id = teamRow[Teams.id].toString(),
                    name = teamRow[Teams.name],
                    record = record,
                    points = points,
                    streak = calculateStreak(matchHistory)
                )
            }
        }
    }

    suspend fun getTeamRoster(teamId: String): List<TeamPlayerResponse>? {
        val uuid = runCatching { UUID.fromString(teamId) }.getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            transaction {
                // Ensure team exists
                if (Teams.selectAll().where { Teams.id eq uuid }.empty()) return@transaction null

                (Players innerJoin PlayerSeasons)
                    .selectAll()
                    .where { PlayerSeasons.teamId eq uuid }
                    .map { playerRow ->
                        val pId = playerRow[Players.id]

                        // Count wins/losses for this specific player
                        val wins = Games.selectAll().where {
                            ((Games.homePlayer1Id eq pId) and (Games.result eq GameResult.HOME)) or
                                    ((Games.awayPlayer1Id eq pId) and (Games.result eq GameResult.AWAY))
                        }.count().toInt()

                        val losses = Games.selectAll().where {
                            ((Games.homePlayer1Id eq pId) and (Games.result eq GameResult.AWAY)) or
                                    ((Games.awayPlayer1Id eq pId) and (Games.result eq GameResult.HOME))
                        }.count().toInt()

                        TeamPlayerResponse(
                            id = pId.toString(),
                            fullName = playerRow[Players.fullName],
                            licenceNr = playerRow[Players.licenceNr],
                            wins = wins,
                            losses = losses
                        )
                    }
            }
        }
    }

    suspend fun getTeamMatches(teamId: String): List<MatchResponse>? {
        val uuid = runCatching { UUID.fromString(teamId) }.getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            transaction {
                if (Teams.selectAll().where { Teams.id eq uuid }.empty()) return@transaction null

                Matches
                    .join(homeTeam, JoinType.INNER, Matches.homeTeamId, homeTeam[Teams.id])
                    .join(awayTeam, JoinType.INNER, Matches.awayTeamId, awayTeam[Teams.id])
                    .selectAll()
                    .where { (Matches.homeTeamId eq uuid) or (Matches.awayTeamId eq uuid) }
                    .orderBy(Matches.playedAt to SortOrder.ASC)
                    .map { it.toMatchResponse() }
            }
        }
    }

    // Helpers
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
        id = this[Matches.id].toString(),
        homeTeam = this[homeTeam[Teams.name]],
        awayTeam = this[awayTeam[Teams.name]],
        homeScore = this[Matches.homeScore]?.toInt(),
        awayScore = this[Matches.awayScore]?.toInt(),
        round = this[Matches.round],
        playedAt = this[Matches.playedAt]?.toString(),
        status = this[Matches.status]
    )
}