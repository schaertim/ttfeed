package com.ttfeed.scraper.knob

class BackfillScraper(client: KnobClient, parser: KnobParser) {
    private val groupScraper   = GroupScraper(client, parser)
    private val matchScraper   = MatchScraper(client, parser)
    private val licenceScraper = OverallPlayerScraper(client, parser)

    /** Full historical backfill: groups → match details + players → licences (all seasons 1989–present) */
    suspend fun run() {
        runGroupScraper()
        runMatchScraper()
        runLicenceScraper()
    }

    /** Single-season backfill — useful for testing or catching up a specific season */
    suspend fun runForSeason(season: String) {
        groupScraper.run(listOf(season))
        matchScraper.run()
        licenceScraper.run()
    }

    /** Scrapes all group structure, teams, players and matches from knob.ch */
    suspend fun runGroupScraper()   = groupScraper.run()

    /** Scrapes individual game results and upserts players encountered in match details */
    suspend fun runMatchScraper()   = matchScraper.run()

    /** Resolves real STT licence numbers via the overall player registry */
    suspend fun runLicenceScraper() = licenceScraper.run()
}
