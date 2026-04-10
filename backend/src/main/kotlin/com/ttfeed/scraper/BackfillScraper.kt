package com.ttfeed.scraper

class BackfillScraper(client: KnobClient, parser: KnobParser) {
    private val groupScraper       = GroupScraper(client, parser)
    private val licenceScraper     = OverallPlayerScraper(client, parser)
    private val gameDetailScraper  = MatchDetailScraper(client, parser)

    /** Full backfill: groups → licenses → game details */
    suspend fun run() {
        runGroupScraper()
        runLicenceScraper()
        runGameDetailScraper()
    }

    /** Scrapes all group structure, teams, players and matches from knob.ch */
    suspend fun runGroupScraper()      = groupScraper.run()

    /** Resolves real STT license numbers via the overall player registry */
    suspend fun runLicenceScraper()    = licenceScraper.run()

    /** Scrapes individual game results for all completed matches */
    suspend fun runGameDetailScraper() = gameDetailScraper.run()
}