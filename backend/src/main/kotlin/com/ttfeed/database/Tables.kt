package com.ttfeed.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object Seasons : Table("season") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 9)
    override val primaryKey = PrimaryKey(id)
}

object Leagues : Table("league") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    override val primaryKey = PrimaryKey(id)
}

object Divisions : Table("division") {
    val id = integer("id").autoIncrement()
    val leagueId = integer("league_id").references(Leagues.id)
    val seasonId = integer("season_id").references(Seasons.id)
    val name = varchar("name", 50)
    val knobGruppe = integer("knob_gruppe").nullable()
    val matchFormat = short("match_format")
    override val primaryKey = PrimaryKey(id)
}

object Clubs : Table("club") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val knobId = integer("knob_id").nullable().uniqueIndex()
    val clickttId = integer("clicktt_id").nullable().uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

object Teams : Table("team") {
    val id = uuid("id").autoGenerate()
    val clubId = uuid("club_id").references(Clubs.id)
    val divisionId = integer("division_id").references(Divisions.id)
    val name = varchar("name", 100)
    val knobId = integer("knob_id").nullable().uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

object Players : Table("player") {
    val id = uuid("id").autoGenerate()
    val licenceNr = varchar("licence_nr", 20).uniqueIndex()
    val knobId = integer("knob_id").nullable().uniqueIndex()
    val clickttId = integer("clicktt_id").nullable().uniqueIndex()
    val fullName = varchar("full_name", 100)
    override val primaryKey = PrimaryKey(id)
}

object PlayerSeasons : Table("player_season") {
    val id = uuid("id").autoGenerate()
    val playerId = uuid("player_id").references(Players.id)
    val teamId = uuid("team_id").references(Teams.id)
    val seasonId = integer("season_id").references(Seasons.id)
    val klass = varchar("klass", 5).nullable()
    override val primaryKey = PrimaryKey(id)
}

object PlayerElos : Table("player_elo") {
    val id = uuid("id").autoGenerate()
    val playerId = uuid("player_id").references(Players.id)
    val seasonId = integer("season_id").references(Seasons.id)
    val eloValue = integer("elo_value")
    val recordedAt = timestampWithTimeZone("recorded_at")
    override val primaryKey = PrimaryKey(id)
}

object Matches : Table("match") {
    val id = uuid("id").autoGenerate()
    val divisionId = integer("division_id").references(Divisions.id)
    val homeTeamId = uuid("home_team_id").references(Teams.id)
    val awayTeamId = uuid("away_team_id").references(Teams.id)
    val round = integer("round").nullable()
    val playedAt = timestampWithTimeZone("played_at").nullable()
    val homeScore = short("home_score").nullable()
    val awayScore = short("away_score").nullable()
    val knobMatchId = integer("knob_match_id").nullable().uniqueIndex()
    val status = varchar("status", 20)
    override val primaryKey = PrimaryKey(id)
}

object Games : Table("game") {
    val id = uuid("id").autoGenerate()
    val matchId = uuid("match_id").references(Matches.id)
    val gameType = varchar("game_type", 10)
    val orderInMatch = short("order_in_match")
    val homePlayer1Id = uuid("home_player1_id").references(Players.id).nullable()
    val homePlayer2Id = uuid("home_player2_id").references(Players.id).nullable()
    val awayPlayer1Id = uuid("away_player1_id").references(Players.id).nullable()
    val awayPlayer2Id = uuid("away_player2_id").references(Players.id).nullable()
    val homeSets = short("home_sets").nullable()
    val awaySets = short("away_sets").nullable()
    val result = varchar("result", 10)
    override val primaryKey = PrimaryKey(id)
}

object GameSets : Table("game_set") {
    val id = uuid("id").autoGenerate()
    val gameId = uuid("game_id").references(Games.id)
    val setNumber = short("set_number")
    val homePoints = short("home_points")
    val awayPoints = short("away_points")
    override val primaryKey = PrimaryKey(id)
}