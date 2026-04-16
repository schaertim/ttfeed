package com.ttfeed.model

enum class GameType {
    SINGLES,
    DOUBLES
}

enum class GameResult {
    HOME,
    AWAY,
    NOT_PLAYED  // Game position exists but was not played (forfeit, or result not yet scraped)
}

enum class MatchStatus {
    SCHEDULED,
    COMPLETED,
    WALKOVER
}

enum class GenderCategory {
    MENS,
    WOMENS
}