package com.ttfeed.scraper.clicktt

import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import com.ttfeed.model.MatchStatus
import com.ttfeed.scraper.clicktt.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ClickTTParser {

    /**
     * Extracts the URL of the Elo-Protokoll (or Ergebnishistorie) tab from a player portrait page.
     * Searches within the content-tabs nav to avoid matching the mobile menu's '#' placeholder links.
     */
    fun extractEloProtokollUrl(portraitHtml: String): String? {
        val doc  = Jsoup.parse(portraitHtml)
        val link = doc.select("ul.content-tabs a:contains(Elo-Protokoll), ul.content-tabs a:contains(Ergebnishistorie)").firstOrNull()
        return link?.attr("href")
    }

    fun parsePlayerPortrait(portraitHtml: String, eloHtml: String?, personId: Int): ClickTTPlayerPortrait {
        val portraitDoc = Jsoup.parse(portraitHtml)
        val eloDoc      = eloHtml?.let { Jsoup.parse(it) }

        // Prefer ELO from the protokoll page; fall back to the portrait page
        val currentElo = eloDoc?.let { parseCurrentElo(it) } ?: parseCurrentElo(portraitDoc)

        // Game history with ELO deltas only exists on the protokoll page
        val games = eloDoc?.let { parseGames(it) } ?: emptyList()

        return ClickTTPlayerPortrait(personId = personId, currentElo = currentElo, games = games)
    }

    /**
     * Parses a club members page, returning the club name and all members with their
     * click-tt person ID, STT licence number, and display name.
     */
    fun parseClubPage(html: String): ClickTTClubPage {
        val doc     = Jsoup.parse(html)
        val members = mutableListOf<ClickTTClubMember>()

        for (row in doc.select("table.result-set tbody tr")) {
            val cells = row.select("td")
            if (cells.size < 3) continue

            val link     = cells[1].select("a[href*='person=']").firstOrNull() ?: continue
            val personId = link.attr("href").let { extractParam(it, "person") }?.toIntOrNull() ?: continue
            val fullName = link.text().trim().takeIf { it.isNotBlank() } ?: continue
            val licence  = cells[2].text().trim().takeIf { it.isNotBlank() } ?: continue

            members.add(ClickTTClubMember(licence = licence, personId = personId, fullName = fullName))
        }

        return ClickTTClubPage(clubName = parseClubName(doc), members = members)
    }

    private fun parseClubName(doc: Document): String? =
        doc.selectFirst("div.content-section h1, h1.page-title, h1")
            ?.text()?.trim()?.takeIf { it.isNotBlank() }

    private fun parseCurrentElo(doc: Document): Int? {
        val eloElement = doc.select("td:containsOwn(Klassierung)").next("td").firstOrNull()
            ?: doc.select("div:containsOwn(Klassierung)").next("div").firstOrNull()

        val text = eloElement?.text()?.trim() ?: return null

        // Format: "A21 (1234)" or just "1234"
        return Regex("\\b([A-Z]\\d{1,2})\\s*\\((\\d+)\\)").find(text)?.groupValues?.last()?.toIntOrNull()
            ?: Regex("\\b(\\d+)\\b").find(text)?.groupValues?.last()?.toIntOrNull()
    }

    private fun parseGames(doc: Document): List<ClickTTGame> {
        val games = mutableListOf<ClickTTGame>()

        // Find all result tables that have an "Opponent" / "Gegner" column header.
        // This skips the info table at the top and correctly targets monthly result tables.
        val tables = doc.select("table.result-set").filter { table ->
            table.select("th").any { th ->
                th.text().contains("Opponent", ignoreCase = true) ||
                        th.text().contains("Gegner", ignoreCase = true)
            }
        }

        for (table in tables) {
            for (row in table.select("tbody tr")) {
                val cells = row.select("td")
                if (cells.size < 7) continue

                val date        = cells[0].text().trim()
                val competition = cells[1].text().trim()
                // cells[2] is the player's own ELO at the time — not stored
                val opponent    = cells[3].text().trim()
                val opponentElo = cells[4].text().trim().toIntOrNull()
                val isWin       = cells[5].select("img[title=Sieg], img[alt=Sieg]").isNotEmpty()
                // ELO delta can be empty for preview games not yet rated
                val eloDelta    = cells[6].text().trim().replace(",", ".").toDoubleOrNull()

                games.add(
                    ClickTTGame(
                        date        = date,
                        competition = competition,
                        opponent    = opponent,
                        opponentElo = opponentElo,
                        eloDelta    = eloDelta,
                        isWin       = isWin
                    )
                )
            }
        }
        return games
    }

    // -------------------------------------------------------------------------
    // Season scraping — league page, group standings, match schedule, match detail
    // -------------------------------------------------------------------------

    /**
     * Parses the league overview page and returns one entry per group link.
     * The category (Herren / Damen / Senioren O40 / …) is taken from the nearest
     * preceding <h2> in document order.
     */
    fun parseLeaguePage(html: String, championship: String): List<ParsedClickTTGroup> {
        val doc    = Jsoup.parse(html)
        val result = mutableListOf<ParsedClickTTGroup>()
        var currentCategory = ""

        // Walk all h2 headings and group links in document order so we can carry
        // the category forward without needing complex ancestor traversal.
        for (el in doc.select("h2, a[href*='groupPage']")) {
            when (el.tagName()) {
                "h2" -> currentCategory = el.text().trim()
                "a"  -> {
                    val href         = el.attr("href")
                    val groupId      = extractParam(href, "group")?.toIntOrNull() ?: continue
                    val divisionName = el.text().trim().takeIf { it.isNotBlank() } ?: continue
                    result += ParsedClickTTGroup(
                        groupId      = groupId,
                        championship = championship,
                        divisionName = divisionName,
                        category     = currentCategory
                    )
                }
            }
        }
        return result
    }

    /**
     * Parses the standings table from a click-tt group page.
     * Column layout: [zone-icon | rank | team-link | played | W | D | L | games "F:A" | +/- | pts]
     */
    fun parseGroupStandings(html: String): List<ParsedClickTTStanding> {
        val doc      = Jsoup.parse(html)
        val result   = mutableListOf<ParsedClickTTStanding>()

        for (row in doc.select("table.result-set tbody tr")) {
            val cells = row.select("td")
            if (cells.size < 9) continue

            val teamLink    = cells[2].selectFirst("a[href*='teamPortrait']") ?: continue
            val teamTableId = extractParam(teamLink.attr("href"), "teamtable")?.toIntOrNull() ?: continue
            val teamName    = teamLink.text().trim().takeIf { it.isNotBlank() } ?: continue
            val position    = cells[1].text().trim().trimEnd('.').toIntOrNull() ?: continue

            val played       = cells[3].text().trim().toIntOrNull() ?: 0
            val won          = cells[4].text().trim().toIntOrNull() ?: 0
            val drawn        = cells[5].text().trim().toIntOrNull() ?: 0
            val lost         = cells[6].text().trim().toIntOrNull() ?: 0

            // Games cell: "48:36"
            val gamesText    = cells[7].text().trim()
            val gamesFor     = gamesText.substringBefore(":").trim().toIntOrNull() ?: 0
            val gamesAgainst = gamesText.substringAfter(":").trim().toIntOrNull() ?: 0

            // Points is the last cell; cells[8] is the +/- diff which we don't store
            val points = cells[9].text().trim().toIntOrNull()
                ?: cells[8].text().trim().toIntOrNull()
                ?: 0

            // click-tt uses "Relegation" as the alt text for the promotion (upward) zone arrow —
            // confusing naming on their part. "Absteiger" marks the actual relegation zone.
            val isPromotion  = cells[0].selectFirst("img[alt='Relegation']")  != null
            val isRelegation = cells[0].selectFirst("img[alt='Absteiger']")   != null

            result += ParsedClickTTStanding(
                teamName     = teamName,
                teamTableId  = teamTableId,
                position     = position,
                played       = played,
                won          = won,
                drawn        = drawn,
                lost         = lost,
                gamesFor     = gamesFor,
                gamesAgainst = gamesAgainst,
                points       = points,
                isPromotion  = isPromotion,
                isRelegation = isRelegation
            )
        }
        return result
    }

    /**
     * Parses the full match schedule (Spielplan) from a click-tt group page
     * requested with displayDetail=meetings.
     *
     * Date rows introduce a new date/time context that carries forward to all
     * subsequent rows in the same date block (marked by td.tabelle-rowspan placeholders).
     */
    fun parseMatchSchedule(html: String): List<ParsedClickTTMatch> {
        val doc     = Jsoup.parse(html)
        val result  = mutableListOf<ParsedClickTTMatch>()
        var currentDate = ""
        var currentTime: String? = null

        val dateRegex = Regex("""^\d{2}\.\d{2}\.\d{4}$""")
        val timeRegex = Regex("""^\d{2}:\d{2}$""")

        for (row in doc.select("table.result-set tbody tr")) {
            val cells = row.select("td")
            if (cells.isEmpty()) continue

            // A row that does NOT start with td.tabelle-rowspan introduces a new date group.
            if (!cells[0].hasClass("tabelle-rowspan")) {
                for (cell in cells) {
                    val text = cell.text().trim()
                    when {
                        text.matches(dateRegex) -> currentDate = text
                        text.matches(timeRegex) -> currentTime = text
                    }
                }
            }

            // Every data row (date-setter or continuation) contains team names and optionally a score link.
            // Team name cells are marked with nowrap in the schedule table.
            val teamCells = cells.filter { it.hasAttr("nowrap") }
            if (teamCells.size < 2) continue

            val homeTeamName = teamCells[0].text().trim().takeIf { it.isNotBlank() } ?: continue
            val awayTeamName = teamCells[1].text().trim().takeIf { it.isNotBlank() } ?: continue

            val scoreLink = row.selectFirst("a[href*='groupMeetingReport']")
            val meetingId = scoreLink?.attr("href")?.let { extractParam(it, "meeting")?.toIntOrNull() }
            val scoreText = scoreLink?.text()?.trim()
            val homeScore = scoreText?.substringBefore(":")?.trim()?.toIntOrNull()
            val awayScore = scoreText?.substringAfter(":")?.trim()?.toIntOrNull()
            val status    = if (meetingId != null) MatchStatus.COMPLETED else MatchStatus.SCHEDULED

            if (currentDate.isEmpty()) continue   // Shouldn't happen but guard against malformed HTML

            result += ParsedClickTTMatch(
                meetingId    = meetingId,
                date         = currentDate,
                time         = currentTime,
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                homeScore    = homeScore,
                awayScore    = awayScore,
                status       = status
            )
        }
        return result
    }

    /**
     * Parses individual game results from a click-tt match detail page.
     *
     * Row layout: [game-label | home-player | home-klass | away-player | away-klass |
     *              set1 | set2 | set3 | set4 | set5 | total-sets | running-score]
     *
     * Singles have one <a href*="person="> per player cell.
     * Doubles have TWO — unlike knob.ch which uses a single combined link.
     */
    fun parseClickTTMatchDetail(html: String, meetingId: Int): ParsedClickTTMatchDetail {
        val doc   = Jsoup.parse(html)
        val games = mutableListOf<ParsedClickTTGame>()
        var order = 0

        for (row in doc.select("table.result-set tbody tr")) {
            val cells = row.select("td")
            // Need at least: label + home-player + home-klass + away-player + away-klass + 1 set + total
            if (cells.size < 7) continue

            val label = cells[0].text().trim()
            // Skip sub-total / total rows (they have no recognisable game label)
            if (label.isBlank() || label.equals("Total", ignoreCase = true)) continue

            val gameType = if (label.contains("Doppel", ignoreCase = true)) GameType.DOUBLES else GameType.SINGLES
            order++

            // --- Player extraction ---
            val homeLinks = cells[1].select("a[href*='person=']")
            val awayLinks = cells[3].select("a[href*='person=']")

            val homePersonId  = homeLinks.getOrNull(0)?.attr("href")?.let { extractParam(it, "person")?.toIntOrNull() }
            val homeName      = homeLinks.getOrNull(0)?.text()?.trim()?.takeIf { it.isNotBlank() }
            val homePersonId2 = homeLinks.getOrNull(1)?.attr("href")?.let { extractParam(it, "person")?.toIntOrNull() }
            val homeName2     = homeLinks.getOrNull(1)?.text()?.trim()?.takeIf { it.isNotBlank() }

            val awayPersonId  = awayLinks.getOrNull(0)?.attr("href")?.let { extractParam(it, "person")?.toIntOrNull() }
            val awayName      = awayLinks.getOrNull(0)?.text()?.trim()?.takeIf { it.isNotBlank() }
            val awayPersonId2 = awayLinks.getOrNull(1)?.attr("href")?.let { extractParam(it, "person")?.toIntOrNull() }
            val awayName2     = awayLinks.getOrNull(1)?.text()?.trim()?.takeIf { it.isNotBlank() }

            // Klass is in cells[2] (home) and cells[4] (away)
            val homeKlass = cells[2].text().trim().takeIf { it.isNotBlank() }
            val awayKlass = cells[4].text().trim().takeIf { it.isNotBlank() }

            // --- Set scores (cells 5..9) ---
            val sets = mutableListOf<ParsedClickTTSet>()
            for (setIndex in 0..4) {
                val cell = cells.getOrNull(5 + setIndex) ?: break
                val text = cell.text().trim()
                if (text.isBlank() || !text.contains(":")) continue
                val hp = text.substringBefore(":").trim().toIntOrNull() ?: continue
                val ap = text.substringAfter(":").trim().toIntOrNull() ?: continue
                sets += ParsedClickTTSet(setNumber = setIndex + 1, homePoints = hp, awayPoints = ap)
            }

            // Total sets from the cell after the 5 set cells (index 10)
            val totalCell = cells.getOrNull(10)
            val totalText = totalCell?.text()?.trim() ?: ""
            val homeSets  = totalText.substringBefore(":").trim().toIntOrNull()
            val awaySets  = totalText.substringAfter(":").trim().toIntOrNull()

            val result = when {
                homeSets != null && awaySets != null && homeSets > awaySets -> GameResult.HOME
                homeSets != null && awaySets != null && awaySets > homeSets -> GameResult.AWAY
                else -> GameResult.NOT_PLAYED
            }

            games += ParsedClickTTGame(
                orderInMatch  = order,
                gameType      = gameType,
                homePersonId  = homePersonId,
                homeName      = homeName,
                homeKlass     = homeKlass,
                homePersonId2 = homePersonId2,
                homeName2     = homeName2,
                awayPersonId  = awayPersonId,
                awayName      = awayName,
                awayKlass     = awayKlass,
                awayPersonId2 = awayPersonId2,
                awayName2     = awayName2,
                homeSets      = homeSets,
                awaySets      = awaySets,
                result        = result,
                sets          = sets
            )
        }

        return ParsedClickTTMatchDetail(meetingId = meetingId, games = games)
    }

    private fun extractParam(url: String, key: String): String? =
        url.split("?", "&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
}
