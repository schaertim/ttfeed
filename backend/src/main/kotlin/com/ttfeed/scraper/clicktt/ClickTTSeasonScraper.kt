package com.ttfeed.scraper.clicktt

/**
 * Orchestrates the full click-tt season scrape for a given season.
 *
 * Phase 1 — ClickTTGroupScraper: league structure, teams, standings, and match schedule
 * Phase 2 — ClickTTMatchScraper: individual game results, set scores, and player upserts
 *
 * Intended usage: run once when a new click-tt season starts (≥ 2025/2026).
 * Re-running is safe — both scrapers skip already-scraped entries.
 */
class ClickTTSeasonScraper(
    private val groupScraper: ClickTTGroupScraper,
    private val matchScraper: ClickTTMatchScraper
) {
    suspend fun run(season: String = "2025/2026") {
        groupScraper.run(season)
        matchScraper.run()
    }

    companion object {
        fun create(): ClickTTSeasonScraper {
            val client = ClickTTClient()
            val parser = ClickTTParser()
            return ClickTTSeasonScraper(
                groupScraper = ClickTTGroupScraper(client, parser),
                matchScraper = ClickTTMatchScraper(client, parser)
            )
        }
    }
}
