package com.ttfeed.scraper.clicktt.model

data class ClickTTGame(
    val date: String,
    val competition: String,
    val opponent: String,
    val opponentElo: Int?,
    val eloDelta: Double?,
    val isWin: Boolean
)

data class ClickTTPlayerPortrait(
    val personId: Int,
    val currentElo: Int?,
    val games: List<ClickTTGame>
)

data class ClickTTClubMember(
    val licence: String,
    val personId: Int,
    val fullName: String
)

data class ClickTTClubPage(
    val clubName: String?,
    val members: List<ClickTTClubMember>
)
