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

    private suspend fun fetchWithRetry(url: String, maxAttempts: Int = 3): String {
        delay(100)
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