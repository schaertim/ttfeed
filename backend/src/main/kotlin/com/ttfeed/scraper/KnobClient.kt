package com.ttfeed.scraper

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class KnobClient {
    private val baseUrl = "https://www.knob.ch/ms/index.php"

    private val client = HttpClient(CIO) {
        followRedirects = true
    }

    suspend fun fetchDivisionPage(gruppeId: Int, season: String? = null): String {
        val url = buildString {
            append("$baseUrl?gruppe=$gruppeId")
            if (season != null) append("&ms=${season.replace("/", "")}")
        }
        return client.get(url).bodyAsText(Charsets.ISO_8859_1)
    }

    suspend fun fetchMatchDetail(gruppeId: Int, matchId: Int): String {
        return client.get("$baseUrl?gruppe=$gruppeId&matchid=$matchId")
            .bodyAsText(Charsets.ISO_8859_1)
    }

    fun close() = client.close()
}