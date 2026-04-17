package com.ttfeed.scraper.knob

class BackfillScraper(client: KnobClient, parser: KnobParser) {
    private val groupScraper        = GroupScraper(client, parser)
    private val licenceScraper      = OverallPlayerScraper(client, parser)
    private val matchDetailScraper  = MatchDetailScraper(client, parser)

    /** Full historical backfill: groups → licences → game details (all seasons 1989–present) */
    suspend fun run() {
        runGroupScraper()
        runLicenceScraper()
        runMatchDetailScraper()
    }

    /** Single-season backfill — useful for testing or catching up a specific season */
    suspend fun runForSeason(season: String) {
        groupScraper.run(listOf(season))
        licenceScraper.run()
        matchDetailScraper.run()
    }

    /** Scrapes all group structure, teams, players and matches from knob.ch */
    suspend fun runGroupScraper()       = groupScraper.run()

    /** Resolves real STT licence numbers via the overall player registry */
    suspend fun runLicenceScraper()     = licenceScraper.run()

    /** Scrapes individual game results for all completed matches */
    suspend fun runMatchDetailScraper() = matchDetailScraper.run()
}
