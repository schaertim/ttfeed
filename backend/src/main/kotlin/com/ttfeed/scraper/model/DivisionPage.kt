package com.ttfeed.scraper.model

data class ParsedTeam(
    val name: String,
    val knobClubId: Int,
    val knobTeamId: Int
)

data class ParsedPlayer(
    val fullName: String,
    val knobId: Int,
    val klass: String,
    val knobClubId: Int,
    val knobTeamId: Int
)

data class ParsedMatch(
    val knobMatchId: Int,
    val round: Int,
    val homeKnobTeamId: Int,
    val awayKnobTeamId: Int,
    val playedAt: String?,       // raw date string, e.g. "Sa. 15.11.2025 14:00"
    val homeScore: Int?,         // null if not yet played
    val awayScore: Int?,
    val status: String           // "scheduled" or "completed"
)

data class ParsedDivisionPage(
    val teams: List<ParsedTeam>,
    val players: List<ParsedPlayer>,
    val matches: List<ParsedMatch>
)