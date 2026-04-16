package com.ttfeed.scraper.clicktt

import com.ttfeed.scraper.clicktt.model.ClickTTGame
import com.ttfeed.scraper.clicktt.model.ClickTTPlayerPortrait
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ClickTTParser {

    fun extractEloProtokollUrl(portraitHtml: String): String? {
        val doc = Jsoup.parse(portraitHtml)
        // NEU: Wir suchen explizit in der ul.content-tabs, damit wir nicht das Handy-Menü (#) erwischen!
        val link = doc.select("ul.content-tabs a:contains(Elo-Protokoll), ul.content-tabs a:contains(Ergebnishistorie)").firstOrNull()
        return link?.attr("href")
    }

    fun parsePlayerPortrait(portraitHtml: String, eloHtml: String?, personId: Int): ClickTTPlayerPortrait {
        val portraitDoc = Jsoup.parse(portraitHtml)
        val eloDoc = eloHtml?.let { Jsoup.parse(it) }

        // Versuche Elo zuerst aus dem Protokoll, dann als Fallback aus dem Portrait zu holen
        val currentElo = (eloDoc?.let { parseCurrentElo(it) }) ?: parseCurrentElo(portraitDoc)

        // Die Spiele (mit Elo-Delta) existieren NUR im Elo-Dokument!
        val games = eloDoc?.let { parseGames(it) } ?: emptyList()

        return ClickTTPlayerPortrait(
            personId = personId,
            currentElo = currentElo,
            games = games
        )
    }

    fun parseClubMembersToMappings(html: String): Map<String, Int> {
        val doc = Jsoup.parse(html)
        val mappings = mutableMapOf<String, Int>()

        // Click-TT packt die Spieler meist in eine table.result-set
        val rows = doc.select("table.result-set tbody tr")

        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 3) continue // Leere/falsche Zeilen überspringen

            // Spalte 2: Name mit Link zur personId
            val link = cells[1].select("a[href*='person=']").firstOrNull()
            val personId = link?.attr("href")?.let { extractParam(it, "person") }?.toIntOrNull()

            // Spalte 3: Die Lizenznummer
            val licence = cells[2].text().trim()

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
        val match = Regex("\\b([A-Z]\\d{1,2})\\s*\\((\\d+)\\)").find(text)
            ?: Regex("\\b(\\d+)\\b").find(text)

        return match?.groupValues?.last()?.toIntOrNull()
    }

    private fun parseGames(doc: Document): List<ClickTTGame> {
        val games = mutableListOf<ClickTTGame>()

        // Genialer Trick: Wir suchen uns ALLE Tabellen auf der Seite,
        // die im Header das Wort "Gegner" haben. So ignorieren wir die
        // Info-Tabelle ganz oben und erwischen zielsicher alle Monats-Tabellen!
        val tables = doc.select("table.result-set").filter { table ->
            table.select("th").any { it.text().contains("Opponent") }
        }

        for (table in tables) {
            val rows = table.select("tbody tr")

            for (row in rows) {
                val cells = row.select("td")

                // Wir brauchen mindestens 7 Spalten (Datum bis Delta)
                if (cells.size < 7) continue

                val date = cells[0].text().trim()
                val competition = cells[1].text().trim()

                // cells[2] ist der eigene Elo-Wert (ignorieren wir für jetzt)

                val opponent = cells[3].text().trim()
                val opponentElo = cells[4].text().trim().toIntOrNull()

                // Sieg/Niederlage Bildchen in Spalte 6
                val isWin = cells[5].select("img[title=Sieg], img[alt=Sieg]").isNotEmpty()

                // Elo-Delta in Spalte 7 (kann leer sein bei Vorschau-Spielen)
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
        }
        return games
    }

    private fun extractParam(url: String, key: String): String? {
        return url.split("?", "&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
    }
}