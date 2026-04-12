package com.ttfeed.scraper.clicktt

import com.ttfeed.scraper.clicktt.model.ClickTTGame
import com.ttfeed.scraper.clicktt.model.ClickTTPlayerPortrait
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ClickTTParser {

    fun extractPersonIdFromSearch(html: String): Int? {
        val doc = Jsoup.parse(html)
        val link = doc.select("a[href*='person=']").firstOrNull() ?: return null
        return extractParam(link.attr("href"), "person")?.toIntOrNull()
    }

    fun parsePlayerPortrait(html: String, personId: Int): ClickTTPlayerPortrait {
        val doc = Jsoup.parse(html)

        return ClickTTPlayerPortrait(
            personId = personId,
            currentElo = parseCurrentElo(doc),
            games = parseGames(doc)
        )
    }

    private fun parseCurrentElo(doc: Document): Int? {
        val eloElement = doc.select("td:containsOwn(Klassierung)").next("td").firstOrNull()
            ?: doc.select("div:containsOwn(Klassierung)").next("div").firstOrNull()

        val text = eloElement?.text()?.trim() ?: return null
        val match = Regex("\\b([A-Z]\\d{1,2})\\s*\\((\\d+)\\)").find(text)
            ?: Regex("\\b(\\d+)\\b").find(text)

        return match?.groupValues?.last()?.toIntOrNull()
    }

    private fun parseGames(doc: Document): List<ClickTTGame> {
        val games = mutableListOf<ClickTTGame>()
        val table = doc.select("table.result-set").firstOrNull() ?: return games
        val rows = table.select("tbody tr")

        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 7) continue

            val date = cells[0].text().trim()
            val competition = cells[1].text().trim()
            val opponent = cells[3].text().trim()
            val opponentElo = cells[4].text().trim().toIntOrNull()

            val isWin = cells[5].select("img[title=Sieg], img[alt=Sieg]").isNotEmpty()

            val eloDelta = cells[6].text().trim().replace(",", ".").toDoubleOrNull()

            games.add(
                ClickTTGame(
                    date = date,
                    competition = competition,
                    opponent = opponent,
                    opponentElo = opponentElo,
                    eloDelta = eloDelta,
                    isWin = isWin
                )
            )
        }
        return games
    }

    private fun extractParam(url: String, key: String): String? {
        return url.split("?", "&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
    }
}