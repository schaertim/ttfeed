package com.ttfeed.scraper

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class KnobClient {
    private val baseUrl = "https://www.knob.ch/ms/index.php"
    private val logger = LoggerFactory.getLogger(KnobClient::class.java)

    private val client = HttpClient(CIO) {
        followRedirects = true
        engine {
            requestTimeout = 30_000
        }
    }

    suspend fun fetchDivisionPage(gruppeId: Int, season: String? = null): String {
        val url = buildString {
            append("$baseUrl?gruppe=$gruppeId")
            if (season != null) append("&ms=${season.replace("/", "")}")
        }
        return fetchWithRetry(url)
    }

    suspend fun fetchMatchDetail(gruppeId: Int, matchId: Int): String {
        return fetchWithRetry("$baseUrl?gruppe=$gruppeId&matchid=$matchId")
    }

    suspend fun fetchLeaguePage(rvid: Int, season: String? = null): String {
        val url = buildString {
            append("$baseUrl?rvid=$rvid")
            if (season != null) append("&ms=${season.replace("/", "")}")
        }
        return fetchWithRetry(url)
    }

    suspend fun fetchMainPage(season: String? = null): String {
        val url = buildString {
            append(baseUrl)
            if (season != null) append("?ms=${season.replace("/", "")}")
        }
        return fetchWithRetry(url)
    }

    private suspend fun fetchWithRetry(url: String, maxAttempts: Int = 3): String {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return client.get(url).bodyAsText(Charsets.ISO_8859_1)
            } catch (e: Exception) {
                lastException = e
                logger.warn("Fetch attempt ${attempt + 1} failed for $url: ${e.message}")
                delay(2000L * (attempt + 1)) // exponential backoff: 2s, 4s, 6s
            }
        }
        throw lastException!!
    }

    fun close() = client.close()
}