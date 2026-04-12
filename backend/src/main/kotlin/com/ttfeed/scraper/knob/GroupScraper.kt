package com.ttfeed.scraper.knob

import com.ttfeed.database.*
import com.ttfeed.scraper.knob.model.ParsedMatch
import com.ttfeed.scraper.knob.model.ParsedPlayer
import com.ttfeed.scraper.knob.model.ParsedStandingRow
import com.ttfeed.scraper.knob.model.ParsedTeam
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class GroupScraper(
    private val client: KnobClient,
    private val parser: KnobParser
) {
    private val logger = LoggerFactory.getLogger(GroupScraper::class.java)

    // Gruppe ID ranges per season, scoped to avoid unnecessary requests.
    // Derived from observed data — knob.ch reuses gruppe IDs across seasons
    // but they cluster within predictable ranges per era.
    private fun gruppeRange(season: String): IntRange {
        val year = season.substringBefore("/").toInt()
        return when {
            year <= 2007 -> 1..50
            year == 2008 -> 50..150
            year <= 2010 -> 1..100
            year <= 2013 -> 1..200
            year == 2014 -> 1..550
            year == 2015 -> 500..1000
            year == 2016 -> 4050..4600
            else         -> 1..600
        }
    }

    suspend fun run() {
        // Ensure all leagues exist before any scraping begins
        transaction {
            FEDERATION_RVIDS.keys.forEach { upsertFederation(it) }
        }

        val seasons = generateSeasons(fromYear = 1989, toYear = 2025)
        logger.info("GroupScraper: ${seasons.size} seasons to scrape")

        for (season in seasons) {
            val range      = gruppeRange(season)
            val seasonYear = season.substringBefore("/").toInt()
            logger.info("Season $season — gruppe range $range")

            // STT pass — national leagues, no rvid
            runPass(season, seasonYear, leagueName = "STT", rvid = null, range = range)

            // Regional passes — regional leagues only exist from 2011/2012 onwards
            if (seasonYear >= 2011) {
                FEDERATION_RVIDS.entries
                    .filter { it.value != null }
                    .forEach { (leagueName, rvid) ->
                        runPass(season, seasonYear, leagueName, rvid, range)
                    }
            }
        }

        logger.info("GroupScraper complete")
    }

    private suspend fun runPass(
        season: String,
        seasonYear: Int,
        leagueName: String,
        rvid: Int?,
        range: IntRange
    ) {
        logger.info("  [$season] $leagueName pass${if (rvid != null) " (rvid=$rvid)" else ""}")
        var found = 0

        for (gruppeId in range) {
            try {
                val html   = client.fetchDivisionPage(gruppeId, season, rvid)
                val result = parser.parseGruppePage(html, gruppeId, seasonYear) ?: continue

                // Cross-check — the page's active league must match this pass's league
                if (result.leagueName != leagueName) continue

                // Skip if already scraped — gruppe IDs are reused across seasons so we
                // must scope the check to both gruppe ID and season name
                if (isAlreadyScraped(gruppeId, season)) {
                    logger.debug("    gruppe=$gruppeId season=$season already scraped, skipping")
                    found++
                    continue
                }

                val page = parser.parseDivisionPage(html)

                transaction {
                    val seasonId    = upsertSeason(season)
                    val federationId = upsertFederation(leagueName)
                    val divisionId  = upsertDivision(result.divisionName, federationId, seasonId)
                    val groupId     = upsertGroup(divisionId, result.groupName, gruppeId)

                    if (page.teams.isNotEmpty()) {
                        val teamIdMap = upsertTeams(page.teams, groupId)
                        upsertPlayerSeasons(page.players, teamIdMap, seasonId)
                        upsertMatches(page.matches, groupId, teamIdMap)
                        upsertStandings(page.standings, groupId, teamIdMap)  // ADD THIS
                        updateGroupZones(groupId, page.promotionSpots, page.relegationSpots)  // ADD THIS
                    }
                }

                found++
                logger.info("    gruppe=$gruppeId → $leagueName / ${result.divisionName} / ${result.groupName} — ${page.teams.size} teams, ${page.matches.size} matches")
            } catch (e: Exception) {
                logger.error("    gruppe=$gruppeId failed: ${e.message}")
            }
        }

        logger.info("  [$season] $leagueName done — $found groups found")
    }

    private fun isAlreadyScraped(gruppeId: Int, season: String): Boolean = transaction {
        (Groups innerJoin Divisions innerJoin Seasons)
            .select(Groups.id)
            .where { (Groups.knobGruppe eq gruppeId) and (Seasons.name eq season) }
            .firstOrNull()
            ?.let { row -> Teams.selectAll().where { Teams.groupId eq row[Groups.id] }.count() > 0 }
            ?: false
    }

    // -------------------------------------------------------------------------
    // DB upserts — all follow the same pattern: insertIgnore then select to get ID
    // -------------------------------------------------------------------------

    private fun upsertSeason(name: String): UUID {
        Seasons.insertIgnore { it[Seasons.name] = name }
        return Seasons.select(Seasons.id)
            .where { Seasons.name eq name }
            .first()[Seasons.id]
    }

    private fun upsertFederation(name: String): UUID {
        Federations.insertIgnore { it[Federations.name] = name }
        return Federations.select(Federations.id)
            .where { Federations.name eq name }
            .first()[Federations.id]
    }

    private fun upsertDivision(name: String, federationId: UUID, seasonId: UUID): UUID {
        Divisions.insertIgnore {
            it[Divisions.name]          = name
            it[Divisions.federationId]  = federationId
            it[Divisions.seasonId]      = seasonId
        }
        return Divisions.select(Divisions.id).where {
            (Divisions.federationId eq federationId) and
                    (Divisions.seasonId     eq seasonId) and
                    (Divisions.name         eq name)
        }.first()[Divisions.id]
    }

    private fun upsertGroup(divisionId: UUID, name: String, knobGruppe: Int): UUID {
        Groups.insertIgnore {
            it[Groups.divisionId] = divisionId
            it[Groups.name]       = name
            it[Groups.knobGruppe] = knobGruppe
        }
        return Groups.select(Groups.id).where {
            (Groups.divisionId eq divisionId) and
                    (Groups.knobGruppe eq knobGruppe)
        }.first()[Groups.id]
    }

    private fun upsertStandings(
        standings: List<ParsedStandingRow>,
        groupId: UUID,
        teamIdMap: Map<Int, UUID>
    ) {
        for (standing in standings) {
            val teamId = teamIdMap[standing.knobTeamId] ?: continue

            val exists = Standings.select(Standings.id)
                .where { (Standings.groupId eq groupId) and (Standings.teamId eq teamId) }
                .firstOrNull() != null

            if (!exists) {
                Standings.insert {
                    it[Standings.groupId]      = groupId
                    it[Standings.teamId]       = teamId
                    it[Standings.position]     = standing.position.toShort()
                    it[Standings.played]       = standing.played.toShort()
                    it[Standings.won]          = standing.won.toShort()
                    it[Standings.drawn]        = standing.drawn.toShort()
                    it[Standings.lost]         = standing.lost.toShort()
                    it[Standings.gamesFor]     = standing.gamesFor.toShort()
                    it[Standings.gamesAgainst] = standing.gamesAgainst.toShort()
                    it[Standings.points]       = standing.points.toShort()
                }
            } else {
                Standings.update({
                    (Standings.groupId eq groupId) and (Standings.teamId eq teamId)
                }) {
                    it[Standings.position]     = standing.position.toShort()
                    it[Standings.played]       = standing.played.toShort()
                    it[Standings.won]          = standing.won.toShort()
                    it[Standings.drawn]        = standing.drawn.toShort()
                    it[Standings.lost]         = standing.lost.toShort()
                    it[Standings.gamesFor]     = standing.gamesFor.toShort()
                    it[Standings.gamesAgainst] = standing.gamesAgainst.toShort()
                    it[Standings.points]       = standing.points.toShort()
                }
            }
        }
    }

    private fun updateGroupZones(groupId: UUID, promotionSpots: Int, relegationSpots: Int) {
        if (promotionSpots > 0 || relegationSpots > 0) {
            Groups.update({ Groups.id eq groupId }) {
                it[Groups.promotionSpots]  = promotionSpots.toShort()
                it[Groups.relegationSpots] = relegationSpots.toShort()
            }
        }
    }

    private fun upsertTeams(teams: List<ParsedTeam>, groupId: UUID): Map<Int, UUID> {
        return teams.associate { team ->
            // Safely strip ONLY trailing numbers, roman numerals, or single letters
            // e.g., "Burgdorf 1" -> "Burgdorf", "Young Stars ZH" -> "Young Stars ZH"
            val cleanClubName = team.name.replace(Regex("""\s+(\d+|[IVX]+|[a-zA-Z])$"""), "").trim()

            Clubs.insertIgnore {
                it[Clubs.name]   = cleanClubName
                // We still save the knobId for history, but we don't rely on it being unique
                it[Clubs.knobId] = team.knobClubId
            }

            // Fetch the club ID by its true NAME, not the unreliable knobId
            val clubId = Clubs.select(Clubs.id)
                .where { Clubs.name eq cleanClubName }
                .first()[Clubs.id]

            // Team knobIds are only unique within a group — check by (knobId, groupId)
            val existingTeamId = Teams.select(Teams.id)
                .where { (Teams.knobId eq team.knobTeamId) and (Teams.groupId eq groupId) }
                .firstOrNull()?.get(Teams.id)

            val teamId = existingTeamId ?: run {
                Teams.insert {
                    it[Teams.name]    = team.name
                    it[Teams.clubId]  = clubId
                    it[Teams.groupId] = groupId
                    it[Teams.knobId]  = team.knobTeamId
                }
                Teams.select(Teams.id)
                    .where { (Teams.knobId eq team.knobTeamId) and (Teams.groupId eq groupId) }
                    .first()[Teams.id]
            }

            team.knobTeamId to teamId
        }
    }

    private fun upsertPlayerSeasons(players: List<ParsedPlayer>, teamIdMap: Map<Int, UUID>, seasonId: UUID) {
        for (player in players) {
            val cleanName = player.fullName.replace(Regex("""\s*\([^)]+\)\s*$"""), "").trim()

            Players.insertIgnore {
                it[Players.fullName]  = cleanName
                it[Players.knobId]    = player.knobId
                it[Players.licenceNr] = "$PLACEHOLDER_LICENCE_PREFIX${player.knobId}"
            }
            val playerId = Players.select(Players.id)
                .where { Players.knobId eq player.knobId }
                .first()[Players.id]

            val teamId = teamIdMap[player.knobTeamId] ?: continue

            PlayerSeasons.insertIgnore {
                it[PlayerSeasons.playerId] = playerId
                it[PlayerSeasons.teamId]   = teamId
                it[PlayerSeasons.seasonId] = seasonId
                it[PlayerSeasons.klass]    = player.klass.takeIf { k -> k.isNotBlank() }
            }
        }
    }

    private fun upsertMatches(matches: List<ParsedMatch>, groupId: UUID, teamIdMap: Map<Int, UUID>) {
        for (match in matches) {
            val homeTeamId = teamIdMap[match.homeKnobTeamId] ?: continue
            val awayTeamId = teamIdMap[match.awayKnobTeamId] ?: continue
            val playedAt   = match.playedAt?.let { parseMatchDate(it) }

            // Match knobIds are only unique within a group — check by (groupId, knobMatchId)
            val exists = Matches.select(Matches.id)
                .where { (Matches.groupId eq groupId) and (Matches.knobMatchId eq match.knobMatchId) }
                .firstOrNull() != null

            if (!exists) {
                Matches.insert {
                    it[Matches.groupId]     = groupId
                    it[Matches.homeTeamId]  = homeTeamId
                    it[Matches.awayTeamId]  = awayTeamId
                    it[Matches.round]       = match.round
                    it[Matches.playedAt]    = playedAt
                    it[Matches.homeScore]   = match.homeScore?.toShort()
                    it[Matches.awayScore]   = match.awayScore?.toShort()
                    it[Matches.knobMatchId] = match.knobMatchId
                    it[Matches.status]      = match.status
                }
            } else if (match.status == MatchStatus.COMPLETED) {
                // Only update if the match has just completed — completed matches are immutable
                Matches.update({
                    (Matches.groupId eq groupId) and (Matches.knobMatchId eq match.knobMatchId) and
                            (Matches.status eq MatchStatus.SCHEDULED)
                }) {
                    it[Matches.homeScore] = match.homeScore?.toShort()
                    it[Matches.awayScore] = match.awayScore?.toShort()
                    it[Matches.status]    = MatchStatus.COMPLETED
                }
            }
        }
    }

    // Date format from knob: "Sa. 12.10.2024 14:00" — strip the day-of-week prefix
    private fun parseMatchDate(raw: String): OffsetDateTime? = try {
        val withoutDay = raw.substringAfter(". ").trim()
        LocalDateTime.parse(withoutDay, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            .atOffset(ZoneOffset.UTC)
    } catch (e: Exception) {
        null
    }
}

// Top-level so it can be reused without instantiating GroupScraper
fun generateSeasons(fromYear: Int, toYear: Int): List<String> =
    (fromYear..toYear).map { year -> "$year/${(year + 1).toString().takeLast(4)}" }