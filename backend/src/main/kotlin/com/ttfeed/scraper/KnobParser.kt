package com.ttfeed.scraper

import com.ttfeed.scraper.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class KnobParser {

    fun parseDivisionPage(html: String): ParsedDivisionPage {
        val doc = Jsoup.parse(html)
        return ParsedDivisionPage(
            teams = parseTeams(doc),
            players = parsePlayers(doc),
            matches = parseMatches(doc)
        )
    }

    private fun parseTeams(doc: Document): List<ParsedTeam> {
        // Teams are in the Team-Rangliste table
        // Each row has a link like: index.php?clubid=49&teamid=1
        return doc.select("table.a02Bar")
            .first()
            ?.nextElementSibling()
            ?.select("tr.psauf, tr.psab, tr.psodd, tr.playerStats, tr.pshl")
            ?.mapNotNull { row ->
                val link = row.select("td.playerName a").firstOrNull() ?: return@mapNotNull null
                val href = link.attr("href")
                val clubId = extractParam(href, "clubid")?.toIntOrNull() ?: return@mapNotNull null
                val teamId = extractParam(href, "teamid")?.toIntOrNull() ?: return@mapNotNull null
                val name = link.text().trim()
                ParsedTeam(name = name, knobClubId = clubId, knobTeamId = teamId)
            }
            ?.distinctBy { it.knobTeamId }
            ?: emptyList()
    }

    private fun parsePlayers(doc: Document): List<ParsedPlayer> {
        // Players are in the Einzel-Rangliste table — identified by a05Bar header
        return doc.select("table.a05Bar")
            .first()
            ?.nextElementSibling() // sort mode row
            ?.nextElementSibling() // stammspieler filter row
            ?.nextElementSibling() // actual player table
            ?.select("tr.psauf, tr.psab, tr.psodd, tr.playerStats, tr.pshl")
            ?.mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 9) return@mapNotNull null

                val playerLink = cells[1].selectFirst("a") ?: return@mapNotNull null
                val teamLink = cells[2].selectFirst("a") ?: return@mapNotNull null

                val playerHref = playerLink.attr("href")
                val teamHref = teamLink.attr("href")

                val knobId = extractParam(playerHref, "gid")?.toIntOrNull() ?: return@mapNotNull null
                val clubId = extractParam(teamHref, "clubid")?.toIntOrNull() ?: return@mapNotNull null
                val teamId = extractParam(teamHref, "teamid")?.toIntOrNull() ?: return@mapNotNull null
                val klass = cells[3].text().trim()

                ParsedPlayer(
                    fullName = playerLink.text().trim(),
                    knobId = knobId,
                    klass = klass,
                    knobClubId = clubId,
                    knobTeamId = teamId
                )
            }
            ?: emptyList()
    }

    private fun parseMatches(doc: Document): List<ParsedMatch> {
        // Match list is under the a04Bar header
        val matchTable = doc.select("table.a04Bar")
            .first()
            ?.nextElementSibling() // listmode row
            ?.nextElementSibling() // actual match table
            ?: return emptyList()

        return matchTable.select("tr.psodd, tr.playerStats, tr.pshl")
            .mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 5) return@mapNotNull null

                // matchid is in the title attribute of the first cell
                val matchId = cells[0].attr("title")
                    .let { extractParamFromTitle(it, "matchid") }
                    ?.toIntOrNull() ?: return@mapNotNull null

                val round = cells[0].text().trim()
                    .split(" ").first()
                    .toIntOrNull() ?: return@mapNotNull null

                // Skip rows that are the expanded match detail header
                val homeTeamLink = cells[2].selectFirst("a") ?: return@mapNotNull null
                val awayTeamLink = cells[3].selectFirst("a") ?: return@mapNotNull null

                val homeTeamId = extractParam(homeTeamLink.attr("href"), "teamid")
                    ?.toIntOrNull() ?: return@mapNotNull null
                val awayTeamId = extractParam(awayTeamLink.attr("href"), "teamid")
                    ?.toIntOrNull() ?: return@mapNotNull null

                val resultCell = cells[4]
                val resultLink = resultCell.selectFirst("a")
                val score = resultLink?.text()?.trim()

                val (homeScore, awayScore, status) = if (score != null && score.contains(":")) {
                    val parts = score.split(":")
                    val h = parts[0].toIntOrNull()
                    val a = parts[1].toIntOrNull()
                    Triple(h, a, "completed")
                } else {
                    Triple(null, null, "scheduled")
                }

                val dateText = cells[1].text().trim().takeIf { it.isNotBlank() }

                ParsedMatch(
                    knobMatchId = matchId,
                    round = round,
                    homeKnobTeamId = homeTeamId,
                    awayKnobTeamId = awayTeamId,
                    playedAt = dateText,
                    homeScore = homeScore,
                    awayScore = awayScore,
                    status = status
                )
            }
    }

    fun parseMatchDetail(html: String, matchId: Int): ParsedMatchDetail {
        val doc = Jsoup.parse(html)
        val games = mutableListOf<ParsedGame>()

        // Game rows are in the detailed match table — identified by the Partie/Details header row
        val gameTable = doc.select("tr:has(td:containsOwn(Partie))")
            .firstOrNull()
            ?.parent()
            ?: return ParsedMatchDetail(matchId, emptyList())

        val gameRows = gameTable.select("tr.psodd, tr.playerStats")
        var order = 1

        for (row in gameRows) {
            val cells = row.select("td")
            if (cells.size < 15) continue

            val label = cells[0].text().trim() // e.g. "A-X", "B-Y", "D-X" (doubles)
            val isDoubles = cells[1].text().contains("/")

            val homePlayerLink = cells[1].selectFirst("a")
            val awayPlayerLink = cells[2].selectFirst("a")

            val homePlayer1Id = homePlayerLink?.attr("href")
                ?.let { extractParam(it, "gid") }?.toIntOrNull()
            val awayPlayer1Id = awayPlayerLink?.attr("href")
                ?.let { extractParam(it, "gid") }?.toIntOrNull()

            // Sets: cells 3..7 contain individual set scores like "5:11"
            val sets = mutableListOf<ParsedSet>()
            var setNumber = 1
            for (i in 3..7) {
                val setText = cells.getOrNull(i)?.text()?.trim() ?: break
                if (setText.isBlank() || setText == "\u00a0") break
                val parts = setText.split(":")
                val homePoints = parts.getOrNull(0)?.toIntOrNull() ?: break
                val awayPoints = parts.getOrNull(1)?.toIntOrNull() ?: break
                sets.add(ParsedSet(setNumber, homePoints, awayPoints))
                setNumber++
            }

            // Sets won: cells 8,9,10 = home:away
            val homeSets = cells.getOrNull(8)?.text()?.trim()?.toIntOrNull()
            val awaySets = cells.getOrNull(10)?.text()?.trim()?.toIntOrNull()

            val result = when {
                homeSets != null && awaySets != null && homeSets > awaySets -> "home"
                homeSets != null && awaySets != null && awaySets > homeSets -> "away"
                homeSets == 0 && awaySets == 0 -> "not_played"
                else -> "not_played"
            }

            games.add(
                ParsedGame(
                    orderInMatch = order++,
                    gameType = if (isDoubles) "doubles" else "singles",
                    homePlayer1KnobId = homePlayer1Id,
                    homePlayer2KnobId = null, // doubles parsing deferred
                    awayPlayer1KnobId = awayPlayer1Id,
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

    fun parseLeagues(html: String): List<Pair<Int, String>> {
        val doc = Jsoup.parse(html)
        return doc.select("ul#rvNav li a")
            .mapNotNull { link ->
                val rvid = extractParam(link.attr("href"), "rvid")?.toIntOrNull() ?: return@mapNotNull null
                val name = link.text().trim()
                Pair(rvid, name)
            }
    }

    fun parseDivisionLinks(html: String): List<Pair<Int, String>> {
        val doc = Jsoup.parse(html)
        val allNavLists = doc.select("ul#mainNav")

        // The first mainNav block is always STT — skip it
        // The second block is the active league's divisions
        // There may be a third block for veterans/women within the same league
        // We skip the first and take the rest
        return allNavLists.drop(1)
            .flatMap { navList ->
                navList.select("li a").mapNotNull { link ->
                    val href = link.attr("href")
                    val gruppe = extractParam(href, "gruppe")?.toIntOrNull()
                        ?: return@mapNotNull null
                    if (gruppe >= 500) return@mapNotNull null
                    val name = link.text().trim().removePrefix("NWTTV ")
                    Pair(gruppe, name)
                }
            }
            .distinctBy { it.first }
    }

    // Extracts ?key=value from a URL string
    private fun extractParam(url: String, key: String): String? {
        return url.split("&", "?")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.substringBefore("&")
    }

    // Extracts key=value from a title attribute like "matchid=20654; sys=-6"
    private fun extractParamFromTitle(title: String, key: String): String? {
        return title.split(";")
            .firstOrNull { it.trim().startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
    }


}