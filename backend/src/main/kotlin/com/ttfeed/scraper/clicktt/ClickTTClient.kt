package com.ttfeed.scraper.clicktt

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cookies.* // Optional, aber gut zu behalten
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class ClickTTClient {
    private val baseUrl = "https://www.click-tt.ch/cgi-bin/WebObjects/nuLigaTTCH.woa/wa"
    private val logger = LoggerFactory.getLogger(ClickTTClient::class.java)

    private val client = HttpClient(CIO) {
        install(HttpCookies)
        followRedirects = true
        engine { requestTimeout = 30_000 }
    }

    suspend fun fetchPlayerPortrait(personId: Int, season: String? = null): String {
        val url = buildString {
            append("$baseUrl/playerPortrait?federation=STT&person=$personId")
            if (season != null) append("&season=${season.replace("/", "%2F")}")
        }
        return fetchWithRetry(url)
    }

    suspend fun fetchUrl(relativeOrAbsoluteUrl: String): String {
        val fullUrl = if (relativeOrAbsoluteUrl.startsWith("http")) relativeOrAbsoluteUrl else "https://www.click-tt.ch$relativeOrAbsoluteUrl"
        return fetchWithRetry(fullUrl)
    }

    suspend fun fetchClubMembersPage(clubId: Int): String {
        val url = "$baseUrl/clubLicenceMembersPage?club=$clubId"
        return fetchWithRetry(url)
    }

    /**
     * Fetches the league overview page listing all groups for a championship.
     * championship format: "{FEDERATION} {YY}/{YY}", e.g. "MTTV 25/26"
     */
    suspend fun fetchLeaguePage(championship: String): String {
        val encoded = championship.replace("/", "%2F").replace(" ", "+")
        return fetchWithRetry("$baseUrl/leaguePage?championship=$encoded&preferredLanguage=German")
    }

    /**
     * Fetches a group page — either the standings view (default) or the full match schedule
     * (displayDetail = "meetings").
     */
    suspend fun fetchGroupPage(championship: String, groupId: Int, displayDetail: String? = null): String {
        val encoded = championship.replace("/", "%2F").replace(" ", "+")
        val url = buildString {
            append("$baseUrl/groupPage?championship=$encoded&group=$groupId&displayTyp=gesamt")
            if (displayDetail != null) append("&displayDetail=$displayDetail")
        }
        return fetchWithRetry(url)
    }

    /**
     * Fetches the individual match detail (Begegnungsbericht) page.
     */
    suspend fun fetchMatchDetail(meetingId: Int, championship: String, groupId: Int): String {
        val encoded = championship.replace("/", "%2F").replace(" ", "+")
        return fetchWithRetry(
            "$baseUrl/groupMeetingReport?meeting=$meetingId&championship=$encoded&group=$groupId"
        )
    }

    private suspend fun fetchWithRetry(url: String, maxAttempts: Int = 3): String {
        delay(10)
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return client.get(url).bodyAsText(Charsets.UTF_8)
            } catch (e: Exception) {
                lastException = e
                logger.warn("Fetch attempt ${attempt + 1} failed for $url: ${e.message}")
                delay(500L * (attempt + 1))
            }
        }
        throw lastException!!
    }

    fun close() = client.close()
}