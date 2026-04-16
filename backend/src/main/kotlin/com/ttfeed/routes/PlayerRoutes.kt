package com.ttfeed.routes

import com.ttfeed.scraper.clicktt.ClickTTSyncService
import com.ttfeed.service.PlayerService
import com.ttfeed.service.SeasonService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

fun Route.playerRoutes() {
    route("/players") {
        get("/search") {
            val name = call.request.queryParameters["name"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing name parameter")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

            val result = PlayerService.search(name, page, size)
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Name must be at least 3 characters")

            call.respond(result)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")

            val player = PlayerService.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Player not found")

            val shouldSync = !player.licenceNr.startsWith("knob:")

            if (shouldSync) {
                // Wichtig: Die Zeile hier drunter testet, ob wir VOR der Coroutine sind
                application.environment.log.info("Triggering background coroutine for player: $id")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        application.environment.log.info("Coroutine started. Fetching season ID...")
                        val currentSeasonId = SeasonService.getCurrentSeasonId()

                        if (currentSeasonId != null) {
                            application.environment.log.info("Season found ($currentSeasonId). Starting click-tt scraper...")
                            ClickTTSyncService.syncSinglePlayer(UUID.fromString(id), currentSeasonId)
                            application.environment.log.info("Scraper finished successfully for $id!")
                        } else {
                            application.environment.log.warn("Could not sync player $id: No active season found.")
                        }
                    } catch (e: Exception) {
                        application.environment.log.error("Background sync failed for player $id", e)
                    }
                }
            }

            call.respond(HttpStatusCode.OK, player.copy(isSyncing = shouldSync))
        }
    }
}