package com.ttfeed.scraper.clicktt

import com.ttfeed.jobs.ClickTtIdBackfillJob

/**
 * Orchestrates the full click-tt season scrape for a given season.
 *
 * Phase 1 — ClickTTGroupScraper: league structure, teams, standings, and match schedule
 * Phase 2 — ClickTtIdBackfillJob: links click-tt person/club IDs to existing knob-scraped records
 * Phase 3 — ClickTTMatchScraper: individual game results, set scores, and player upserts
 *
 * The backfill job must run between phases 1 and 3 so that players encountered in match details
 * are already linked to their knob records, avoiding duplicate player rows.
 */
class ClickTTSeasonScraper(
    private val groupScraper:  ClickTTGroupScraper,
    private val backfillJob:   ClickTtIdBackfillJob,
    private val matchScraper:  ClickTTMatchScraper
) {
    suspend fun run(season: String = "2025/2026", federations: Collection<String>? = null) {
        if (federations != null) groupScraper.run(season, federations) else groupScraper.run(season)
        backfillJob.run()
        matchScraper.run()
    }

    companion object {
        fun create(): ClickTTSeasonScraper {
            val client = ClickTTClient()
            val parser = ClickTTParser()
            return ClickTTSeasonScraper(
                groupScraper = ClickTTGroupScraper(client, parser),
                backfillJob  = ClickTtIdBackfillJob(client, parser),
                matchScraper = ClickTTMatchScraper(client, parser)
            )
        }
    }
}
