package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.GameResponse
import com.ttfeed.model.MatchDetailResponse
import com.ttfeed.model.MatchResponse
import com.ttfeed.model.SetResponse
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias

object MatchService {
    private val homeTeam   = Teams.alias("home_team")
    private val awayTeam   = Teams.alias("away_team")
    private val homePlayer = Players.alias("home_player")
    private val awayPlayer = Players.alias("away_player")

    suspend fun getForGroup(groupId: String): List<MatchResponse>? {
        val uuid = groupId.toUuidOrNull() ?: return null
        return dbQuery {
            Matches
                .join(homeTeam, JoinType.INNER, Matches.homeTeamId, homeTeam[Teams.id])
                .join(awayTeam, JoinType.INNER, Matches.awayTeamId, awayTeam[Teams.id])
                .select(
                    Matches.id,
                    homeTeam[Teams.name],
                    awayTeam[Teams.name],
                    Matches.homeScore,
                    Matches.awayScore,
                    Matches.round,
                    Matches.playedAt,
                    Matches.status
                )
                .where { Matches.groupId eq uuid }
                .map { it.toMatchResponse() }
        }
    }

    suspend fun getById(matchId: String): MatchDetailResponse? {
        val uuid = matchId.toUuidOrNull() ?: return null
        return dbQuery {
            // Step 1 — fetch the match with team names
            val matchRow = Matches
                .join(homeTeam, JoinType.INNER, Matches.homeTeamId, homeTeam[Teams.id])
                .join(awayTeam, JoinType.INNER, Matches.awayTeamId, awayTeam[Teams.id])
                .select(
                    Matches.id,
                    homeTeam[Teams.name],
                    awayTeam[Teams.name],
                    Matches.homeScore,
                    Matches.awayScore,
                    Matches.round,
                    Matches.playedAt,
                    Matches.status
                )
                .where { Matches.id eq uuid }
                .firstOrNull() ?: return@dbQuery null

            // Step 2 — fetch all games with player names
            val gameRows = Games
                .join(homePlayer, JoinType.LEFT, Games.homePlayer1Id, homePlayer[Players.id])
                .join(awayPlayer, JoinType.LEFT, Games.awayPlayer1Id, awayPlayer[Players.id])
                .select(
                    Games.id,
                    Games.orderInMatch,
                    Games.competitionName,
                    Games.gameType,
                    Games.homeSets,
                    Games.awaySets,
                    Games.result,
                    homePlayer[Players.fullName],
                    awayPlayer[Players.fullName],
                    Games.homePlayer1EloDelta,
                    Games.awayPlayer1EloDelta,
                )
                .where { Games.matchId eq uuid }
                .orderBy(Games.orderInMatch to SortOrder.ASC)

            // Step 3 — fetch all sets for all games in one query
            val gameIds = gameRows.map { it[Games.id] }
            val setsByGame = if (gameIds.isEmpty()) emptyMap() else {
                GameSets
                    .select(GameSets.gameId, GameSets.setNumber, GameSets.homePoints, GameSets.awayPoints)
                    .where { GameSets.gameId inList gameIds }
                    .orderBy(GameSets.setNumber to SortOrder.ASC)
                    .groupBy { it[GameSets.gameId] }
            }

            // Step 4 — assemble nested response
            val games = gameRows.map { gameRow ->
                val gameId = gameRow[Games.id]
                val sets   = setsByGame[gameId]?.map { setRow ->
                    SetResponse(
                        setNumber  = setRow[GameSets.setNumber].toInt(),
                        homePoints = setRow[GameSets.homePoints].toInt(),
                        awayPoints = setRow[GameSets.awayPoints].toInt()
                    )
                } ?: emptyList()

                GameResponse(
                    id                  = gameId.toString(),
                    orderInMatch        = gameRow[Games.orderInMatch]?.toInt(),
                    competitionName     = gameRow[Games.competitionName],
                    gameType            = gameRow[Games.gameType],
                    homePlayerName      = gameRow[homePlayer[Players.fullName]],
                    awayPlayerName      = gameRow[awayPlayer[Players.fullName]],
                    homeSets            = gameRow[Games.homeSets]?.toInt(),
                    awaySets            = gameRow[Games.awaySets]?.toInt(),
                    result              = gameRow[Games.result],
                    homePlayer1EloDelta = gameRow[Games.homePlayer1EloDelta],
                    awayPlayer1EloDelta = gameRow[Games.awayPlayer1EloDelta],
                    sets                = sets
                )
            }

            MatchDetailResponse(
                id        = matchRow[Matches.id].toString(),
                homeTeam  = matchRow[homeTeam[Teams.name]],
                awayTeam  = matchRow[awayTeam[Teams.name]],
                homeScore = matchRow[Matches.homeScore]?.toInt(),
                awayScore = matchRow[Matches.awayScore]?.toInt(),
                round     = matchRow[Matches.round],
                playedAt  = matchRow[Matches.playedAt]?.toString(),
                status    = matchRow[Matches.status],
                games     = games
            )
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
