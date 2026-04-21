package com.ttfeed.model

import kotlinx.serialization.Serializable


@Serializable
data class TeamSummaryResponse(
    val id: String,
    val name: String,
    val record: String,
    val points: Int,
    val streak: String
)

@Serializable
data class TeamPlayerResponse(
    val id: String,
    val fullName: String,
    val licenceNr: String,
    val wins: Int,
    val losses: Int
)

@Serializable
data class GroupResponse(
    val id: String,
    val name: String,
    val federation: String,
    val season: String,
    val promotionSpots: Int?,
    val relegationSpots: Int?,
)
@Serializable
data class StandingResponse(
    val teamId: String,
    val team: String,
    val position: Int,
    val played: Int,
    val won: Int,
    val lost: Int,
    val drawn: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val points: Int,
)

@Serializable
data class MatchResponse(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val round: String?,
    val status: MatchStatus,
    val playedAt: String?,
)

@Serializable
data class MatchDetailResponse(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val round: String?,
    val playedAt: String?,
    val status: MatchStatus,
    val games: List<GameResponse>
)

@Serializable
data class GameResponse(
    val id: String,
    val orderInMatch: Int?,
    val competitionName: String?,
    val gameType: GameType,
    val homePlayerName: String?,
    val awayPlayerName: String?,
    val homeSets: Int?,
    val awaySets: Int?,
    val result: GameResult,
    val homePlayer1EloDelta: Double? = null,
    val awayPlayer1EloDelta: Double? = null,
    val sets: List<SetResponse>
)

@Serializable
data class SetResponse(
    val setNumber: Int,
    val homePoints: Int,
    val awayPoints: Int,
)

@Serializable
data class SeasonResponse(
    val id: String,
    val name: String,
)

@Serializable
data class FederationResponse(
    val id: String,
    val name: String,
)

@Serializable
data class PlayerResponse(
    val id: String,
    val fullName: String,
    val licenceNr: String,
    val currentClubName: String? = null,
    val klass: String? = null,
    val currentElo: Int? = null,
    val isSyncing: Boolean = false
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
)