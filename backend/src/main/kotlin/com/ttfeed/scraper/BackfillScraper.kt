package com.ttfeed.scraper

import com.ttfeed.database.Divisions
import com.ttfeed.database.Games
import com.ttfeed.database.Matches
import com.ttfeed.database.Seasons
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.and
import org.slf4j.LoggerFactory

class BackfillScraper(
    private val client: KnobClient,
    private val parser: KnobParser,
    private val scraper: KnobScraper
) {
    private val logger = LoggerFactory.getLogger(BackfillScraper::class.java)

    // All seasons from 1989/1990 to present
    private val allSeasons = generateSeasons(1989, 2025)

    // STT divisions are on the main page, not under a rvid
    private val sttDivisions = listOf(
        Triple(1, "STT", "L Men"),
        Triple(2, "STT", "NLB"),
        Triple(4, "STT", "NLC"),
        Triple(8, "STT", "L Women"),
        Triple(9, "STT", "NLB Women")
    )

    suspend fun run() {
        logger.info("Starting historical backfill — ${allSeasons.size} seasons")

        for (season in allSeasons) {
            logger.info("Processing season $season")
            scrapeSeasonAllLeagues(season)
        }

        logger.info("Backfill complete")
    }

    private suspend fun scrapeSeasonAllLeagues(season: String) {
        // Scrape STT divisions
        for ((gruppeId, leagueName, divisionName) in sttDivisions) {
            scrapeDivisionSafe(gruppeId, season, leagueName, divisionName)
        }

        // Discover and scrape all regional leagues
        val mainHtml = client.fetchMainPage(season)
        val leagues = parser.parseLeagues(mainHtml)

        for ((rvid, leagueName) in leagues) {
            delay(500) // be polite to knob.ch
            val leagueHtml = client.fetchLeaguePage(rvid, season)
            val divisions = parser.parseDivisionLinks(leagueHtml)

            for ((gruppeId, divisionName) in divisions) {
                scrapeDivisionSafe(gruppeId, season, leagueName, divisionName)
            }
        }
    }

    private suspend fun scrapeDivisionSafe(
        gruppeId: Int,
        season: String,
        leagueName: String,
        divisionName: String
    ) {
        try {
            delay(300)
            scraper.scrapeDivision(gruppeId, season, leagueName, divisionName)

            val seasonYear = season.substringBefore("/").toIntOrNull() ?: 0
            if (seasonYear >= 2000) {
                scrapeCompletedMatches()
            }
        } catch (e: Exception) {
            logger.error("Failed scraping division gruppe=$gruppeId season=$season: ${e.message}")
        }
    }

    private suspend fun scrapeCompletedMatches() {
        val matchTriples = getCompletedMatchesWithoutGames()
        logger.info("Scraping ${matchTriples.size} match details")
        for ((gruppe, matchId, season) in matchTriples) {
            try {
                delay(300)
                scraper.scrapeMatchDetail(gruppe, matchId, season)
            } catch (e: Exception) {
                logger.error("Failed scraping match detail matchid=$matchId: ${e.message}")
            }
        }
    }

    private fun getCompletedMatchesWithoutGames(): List<Triple<Int, Int, String>> {
        return org.jetbrains.exposed.sql.transactions.transaction {
            val gamesSubquery = Games.select(Games.matchId)

            (Matches innerJoin Divisions innerJoin Seasons)
                .select(Matches.knobMatchId, Divisions.knobGruppe, Seasons.name)
                .where {
                    (Matches.status eq "completed") and
                            (Matches.id notInSubQuery gamesSubquery) and
                            (Divisions.knobGruppe.isNotNull())
                }
                .mapNotNull { row ->
                    val matchId = row[Matches.knobMatchId] ?: return@mapNotNull null
                    val gruppe = row[Divisions.knobGruppe] ?: return@mapNotNull null
                    val season = row[Seasons.name]
                    Triple(gruppe, matchId, season)
                }
        }
    }

    private fun generateSeasons(fromYear: Int, toYear: Int): List<String> {
        return (fromYear..toYear).map { year ->
            "${year}/${(year + 1).toString().takeLast(4)}"
        }
    }

    suspend fun runSingleSeason(season: String) {
        logger.info("Scraping single season: $season")
        scrapeSeasonAllLeagues(season)
        logger.info("Season $season complete")
    }
}