package com.ttfeed.scraper.clicktt

import com.ttfeed.scraper.clicktt.model.ClickTTGame
import com.ttfeed.scraper.clicktt.model.ClickTTPlayerPortrait
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
     * Parses the club members page and returns a map of STT licence number → click-tt person ID.
     */
    fun parseClubMembersToMappings(html: String): Map<String, Int> {
        val doc      = Jsoup.parse(html)
        val mappings = mutableMapOf<String, Int>()

        for (row in doc.select("table.result-set tbody tr")) {
            val cells    = row.select("td")
            if (cells.size < 3) continue

            val personId = cells[1].select("a[href*='person=']").firstOrNull()
                ?.attr("href")?.let { extractParam(it, "person") }?.toIntOrNull()
            val licence  = cells[2].text().trim()

            if (personId != null && licence.isNotEmpty()) {
                mappings[licence] = personId
            }
        }
        return mappings
    }

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

    private fun extractParam(url: String, key: String): String? =
        url.split("?", "&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
}
