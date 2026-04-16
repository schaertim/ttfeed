package com.ttfeed.scraper.knob

import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import com.ttfeed.model.MatchStatus
import com.ttfeed.scraper.knob.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class KnobParser {

    // -------------------------------------------------------------------------
    // Gruppe page — identifies which division/league a gruppe belongs to
    // -------------------------------------------------------------------------

    /**
     * Parses a gruppe page and returns structural metadata about it.
     * Returns null if the page does not correspond to the requested gruppe
     * (knob.ch redirects invalid gruppe IDs to a default page).
     *
     * [seasonYear] is used to determine league assignment — before 2011 all
     * groups were STT regardless of nav block position.
     */
    fun parseGruppePage(html: String, requestedGruppe: Int, seasonYear: Int): GruppePageResult? {
        val doc = Jsoup.parse(html)

        // Find the red (active) nav item and verify it matches the requested gruppe
        var selectedGruppe     = -1
        var selectedBlockIndex = -1

        doc.select("ul#mainNav").forEachIndexed { blockIdx, navList ->
            navList.select("li a").forEach { link ->
                if (link.selectFirst("font[color=red]") != null) {
                    val gruppe = extractParam(link.attr("href"), "gruppe")?.toIntOrNull()
                        ?: return@forEach
                    selectedGruppe     = gruppe
                    selectedBlockIndex = blockIdx
                }
            }
        }

        if (selectedGruppe == -1 || selectedGruppe != requestedGruppe) return null

        // Content header format: "NLB / Gruppe 2" or just "NLA"
        val contentHeader = doc.select("table.a01Bar td.playerStatTitle")
            .firstOrNull()?.text()?.trim()
            ?: return null

        val (divisionName, groupName) = if (contentHeader.contains("/")) {
            val division  = contentHeader.substringBefore("/").trim()
            val groupPart = contentHeader.substringAfter("/").trim()
            // Numeric group parts get a prefix: "2" → "Gruppe 2"
            val group = if (groupPart.toIntOrNull() != null) "Gruppe $groupPart" else groupPart
            division to group
        } else {
            contentHeader to "Gruppe 1"
        }

        if (divisionName.isBlank()) return null

        return GruppePageResult(
            gruppeId = selectedGruppe,
            leagueName = resolveLeague(doc, selectedBlockIndex, seasonYear),
            divisionName = divisionName,
            groupName = groupName
        )
    }

    // -------------------------------------------------------------------------
    // Division page — teams, players, matches
    // -------------------------------------------------------------------------

    fun parseDivisionPage(html: String): ParsedDivisionPage {
        val doc       = Jsoup.parse(html)
        val standings = parseStandings(html)
        return ParsedDivisionPage(
            teams = parseTeams(doc),
            players = parsePlayers(doc),
            matches = parseMatches(doc),
            standings = standings.standings,
            promotionSpots = standings.promotionSpots,
            relegationSpots = standings.relegationSpots
        )
    }

    private fun parseStandings(html: String): ParsedStandingsPage {
        val doc   = Jsoup.parse(html)

        // The standings table is the first pTitle table — it has the team ranking header
        val table = doc.select("table.pTitle").firstOrNull {
            it.selectFirst("tr td:contains(Rang)") != null ||
                    it.select("tr.psauf, tr.psab, tr.psodd, tr.playerStats").isNotEmpty()
        } ?: return ParsedStandingsPage(emptyList(), 0, 0)

        val rows             = table.select("tr")
        val standings        = mutableListOf<ParsedStandingRow>()
        var promotionSpots   = 0
        var relegationStart  = Int.MAX_VALUE
        var position         = 1

        for (row in rows) {
            // Promotion zone separator — rows above this are promotion spots
            if (row.hasClass("auf")) {
                promotionSpots = position - 1
                continue
            }
            // Relegation zone separator — rows from here down are relegation spots
            if (row.hasClass("ab")) {
                relegationStart = position
                continue
            }

            val isStandingRow = row.hasClass("psauf") || row.hasClass("psab") ||
                    row.hasClass("psodd") || row.hasClass("playerStats")
            if (!isStandingRow) continue

            val cells = row.select("td")
            if (cells.size < 9) continue

            val teamLink   = cells[1].selectFirst("a") ?: continue
            val knobTeamId = extractParam(teamLink.attr("href"), "teamid")?.toIntOrNull() ?: continue

            val played       = cells[3].text().trim().toIntOrNull() ?: continue
            val won          = cells[4].text().trim().toIntOrNull() ?: continue
            val drawn        = cells[5].text().trim().toIntOrNull() ?: continue
            val lost         = cells[6].text().trim().toIntOrNull() ?: continue

            // SiegVerh column format: "105:35"
            val siegVerh     = cells[7].text().trim()
            val gamesFor     = siegVerh.substringBefore(":").trim().toIntOrNull() ?: 0
            val gamesAgainst = siegVerh.substringAfter(":").trim().toIntOrNull() ?: 0

            // Points are in a bold td — cells[9]
            val points = cells[9].text().trim().toIntOrNull() ?: continue

            standings.add(
                ParsedStandingRow(
                    position = position,
                    knobTeamId = knobTeamId,
                    played = played,
                    won = won,
                    drawn = drawn,
                    lost = lost,
                    gamesFor = gamesFor,
                    gamesAgainst = gamesAgainst,
                    points = points
                )
            )
            position++
        }

        val relegationSpots = if (relegationStart == Int.MAX_VALUE) 0
        else standings.size - relegationStart + 1

        return ParsedStandingsPage(standings, promotionSpots, relegationSpots)
    }

    private fun resolveLeague(doc: Document, selectedBlockIndex: Int, seasonYear: Int): String {
        if (seasonYear < 2011) return "STT"   // regional leagues didn't exist before 2011
        if (selectedBlockIndex == 0) return "STT"

        // Regional — the active league is the grayed-out item (no <a>) in the rvNav
        val rvNav      = doc.selectFirst("ul#rvNav") ?: return "NWTTV"
        val grayedItem = rvNav.select("li").firstOrNull { li ->
            li.selectFirst("a") == null && li.text().trim().isNotBlank()
        }
        return grayedItem?.text()?.trim() ?: "NWTTV"
    }

    private fun parseTeams(doc: Document): List<ParsedTeam> {
        return doc.select("table.a02Bar").first()
            ?.nextElementSibling()
            ?.select("tr.psauf, tr.psab, tr.psodd, tr.playerStats, tr.pshl")
            ?.mapNotNull { row ->
                val link   = row.select("td.playerName a").firstOrNull() ?: return@mapNotNull null
                val href   = link.attr("href")
                val clubId = extractParam(href, "clubid")?.toIntOrNull() ?: return@mapNotNull null
                val teamId = extractParam(href, "teamid")?.toIntOrNull() ?: return@mapNotNull null
                ParsedTeam(name = link.text().trim(), knobClubId = clubId, knobTeamId = teamId)
            }
            ?.distinctBy { it.knobTeamId }
            ?: emptyList()
    }

    private fun parsePlayers(doc: Document): List<ParsedPlayer> {
        // The player table (a05Bar) is followed by two filter tables before the actual data table
        val playerTable = doc.select("table.a05Bar").first()
            ?.nextElementSibling()   // sort mode toggle table
            ?.nextElementSibling()   // stammspieler filter table
            ?.nextElementSibling()   // actual player ranking table

        return playerTable
            ?.select("tr.psauf, tr.psab, tr.psodd, tr.playerStats, tr.pshl")
            ?.mapNotNull { row ->
                val cells = row.select("td")
                // Expected columns: rank | player link | team link | klass | AnzMS | ...
                if (cells.size < 9) return@mapNotNull null

                val playerLink = cells[1].selectFirst("a") ?: return@mapNotNull null
                val teamLink   = cells[2].selectFirst("a") ?: return@mapNotNull null
                val knobId     = extractParam(playerLink.attr("href"), "gid")?.toIntOrNull()
                    ?: return@mapNotNull null
                val clubId     = extractParam(teamLink.attr("href"), "clubid")?.toIntOrNull()
                    ?: return@mapNotNull null
                val teamId     = extractParam(teamLink.attr("href"), "teamid")?.toIntOrNull()
                    ?: return@mapNotNull null

                ParsedPlayer(
                    fullName = playerLink.text().trim(),
                    knobId = knobId,
                    klass = cells[3].text().trim(),
                    knobClubId = clubId,
                    knobTeamId = teamId
                )
            }
            ?: emptyList()
    }

    private fun parseMatches(doc: Document): List<ParsedMatch> {
        // The match table is identified by a header row containing "Runde"
        // This handles both layouts (with and without Vorrunde column)
        val matchTable = doc.select("tr:has(td:containsOwn(Runde))")
            .firstOrNull()?.parent()?.parent()
            ?: return emptyList()

        return matchTable.select("tr.psodd, tr.playerStats, tr.pshl")
            .mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 5) return@mapNotNull null

                val matchId = cells[0].attr("title")
                    .let { extractParamFromTitle(it, "matchid") }
                    ?.toIntOrNull() ?: return@mapNotNull null

                // Round is a number ("8 ( 1 )") or cup label ("Viertelfinal") — store normalised
                val roundRaw = cells[0].text().trim()
                val round    = roundRaw.split(" ").first().toIntOrNull()?.toString()
                    ?: roundRaw.takeIf { it.isNotBlank() }

                val homeTeamId = extractParam(
                    cells[2].selectFirst("a")?.attr("href") ?: return@mapNotNull null,
                    "teamid"
                )?.toIntOrNull() ?: return@mapNotNull null

                val awayTeamId = extractParam(
                    cells[3].selectFirst("a")?.attr("href") ?: return@mapNotNull null,
                    "teamid"
                )?.toIntOrNull() ?: return@mapNotNull null

                val scoreText = cells[4].selectFirst("a")?.text()?.trim()
                val (homeScore, awayScore, status) = parseScore(scoreText)

                ParsedMatch(
                    knobMatchId = matchId,
                    round = round,
                    homeKnobTeamId = homeTeamId,
                    awayKnobTeamId = awayTeamId,
                    playedAt = cells[1].text().trim().takeIf { it.isNotBlank() },
                    homeScore = homeScore,
                    awayScore = awayScore,
                    status = status
                )
            }
    }

    private fun parseScore(scoreText: String?): Triple<Int?, Int?, MatchStatus> {
        if (scoreText == null || !scoreText.contains(":")) {
            return Triple(null, null, MatchStatus.SCHEDULED)
        }
        val parts = scoreText.split(":")
        val home  = parts[0].toIntOrNull()
        val away  = parts[1].toIntOrNull()
        return if (home != null && away != null) Triple(home, away, MatchStatus.COMPLETED)
        else Triple(null, null, MatchStatus.COMPLETED)
    }

    // -------------------------------------------------------------------------
    // Match detail page — individual game and set results
    // -------------------------------------------------------------------------

    fun parseMatchDetail(html: String, matchId: Int): ParsedMatchDetail {
        val doc       = Jsoup.parse(html)
        val gameTable = doc.select("tr:has(td:containsOwn(Partie))")
            .firstOrNull()?.parent()
            ?: return ParsedMatchDetail(matchId, emptyList())

        val games = mutableListOf<ParsedGame>()
        var order = 1

        for (row in gameTable.select("tr.psodd, tr.playerStats")) {
            val cells = row.select("td")

            // Minimum viable row: label + home player + away player + set score cells + running totals
            // Rows without individual set scores use colspan=5 for the set area — still at least 9 cells
            if (cells.size < 9) continue

            val isDoubles     = cells[1].text().contains("/")
            val homePlayerGid = cells[1].selectFirst("a")?.attr("href")
                ?.let { extractParam(it, "gid") }?.toIntOrNull()
            val awayPlayerGid = cells[2].selectFirst("a")?.attr("href")
                ?.let { extractParam(it, "gid") }?.toIntOrNull()

            // Cells 3..7 hold individual set scores (e.g. "11:8") when available.
            // When unavailable they collapse into a single colspan=5 td containing "&nbsp;".
            val sets = parseSetScores(cells, fromIndex = 3, toIndex = 7)

            // Set count totals: home at index 8, ":" separator at 9, away at 10
            val homeSets = cells.getOrNull(8)?.text()?.trim()?.toIntOrNull()
            val awaySets = cells.getOrNull(10)?.text()?.trim()?.toIntOrNull()

            val result = when {
                homeSets != null && awaySets != null && homeSets > awaySets -> GameResult.HOME
                homeSets != null && awaySets != null && awaySets > homeSets -> GameResult.AWAY
                else                                                         -> GameResult.NOT_PLAYED
            }

            games.add(
                ParsedGame(
                    orderInMatch = order++,
                    gameType = if (isDoubles) GameType.DOUBLES else GameType.SINGLES,
                    homePlayer1KnobId = homePlayerGid,
                    homePlayer2KnobId = null,
                    awayPlayer1KnobId = awayPlayerGid,
                    awayPlayer2KnobId = null,
                    homeSets = homeSets,
                    awaySets = awaySets,
                    result = result,
                    sets = sets
                )
            )
        }

        return ParsedMatchDetail(matchId, games)
    }

    private fun parseSetScores(cells: List<Element>, fromIndex: Int, toIndex: Int): List<ParsedSet> {
        val sets = mutableListOf<ParsedSet>()
        for (i in fromIndex..toIndex) {
            val text = cells.getOrNull(i)?.text()?.trim() ?: break
            if (text.isBlank() || text == "\u00a0") break
            val parts = text.split(":")
            val home  = parts.getOrNull(0)?.toIntOrNull() ?: break
            val away  = parts.getOrNull(1)?.toIntOrNull() ?: break
            sets.add(ParsedSet(setNumber = sets.size + 1, homePoints = home, awayPoints = away))
        }
        return sets
    }

    // -------------------------------------------------------------------------
    // Overall player registry (?overall=5)
    // -------------------------------------------------------------------------

    /**
     * Parses the nationwide licensed player list for a season.
     * Returns players that have a valid STT license number.
     * Rows with "nicht STT lizenziert" or a blank license are skipped.
     */
    fun parseOverallPlayers(html: String): List<ParsedLicensedPlayer> {
        val doc   = Jsoup.parse(html)
        val table = doc.selectFirst("table.pTitle") ?: return emptyList()

        return table.select("tr.psauf, tr.psab")
            .mapNotNull { row ->
                val cells    = row.select("td")
                if (cells.size < 2) return@mapNotNull null
                val fullName = cells[0].text().trim()
                if (fullName.isBlank()) return@mapNotNull null
                val licence  = cells[1].text().trim()
                if (licence.isBlank() || licence == "-" || licence == "0" || licence.contains("nicht")) {
                    return@mapNotNull null
                }
                ParsedLicensedPlayer(fullName = fullName, licenceNr = licence)
            }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun extractParam(url: String, key: String): String? =
        url.split("&", "?")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.substringBefore("&")

    private fun extractParamFromTitle(title: String, key: String): String? =
        title.split(";")
            .firstOrNull { it.trim().startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
}