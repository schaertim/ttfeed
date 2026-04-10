package com.ttfeed.model

import kotlinx.serialization.Serializable


@Serializable
data class DivisionResponse(
    val id: String,
    val name: String,
    val federation: String,
    val season: String,
)
@Serializable
data class GroupResponse(
    val id: String,
    val name: String,
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
    val playedAt: String?,
    val status: String,
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
    val status: String,
    val games: List<GameResponse>
)

@Serializable
data class GameResponse(
    val id: String,
    val orderInMatch: Int,
    val gameType: String,
    val homePlayerName: String?,
    val awayPlayerName: String?,
    val homeSets: Int?,
    val awaySets: Int?,
    val result: String,
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
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
)