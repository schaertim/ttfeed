package com.ttfeed.routes

import com.ttfeed.scraper.clicktt.ClickTTSyncService
import com.ttfeed.service.PlayerService
import com.ttfeed.service.SeasonService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing player id")

            val player = PlayerService.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Player not found")

            // Trigger a background click-tt sync for real licensed players (not knob placeholders)
            val shouldSync = !player.licenceNr.startsWith("knob:")
            if (shouldSync) {
                // Scoped to the application lifecycle — cancelled cleanly on shutdown
                call.application.launch(Dispatchers.IO) {
                    try {
                        val currentSeasonId = SeasonService.getCurrentSeasonId()
                        if (currentSeasonId != null) {
                            ClickTTSyncService.syncPlayer(UUID.fromString(id), currentSeasonId)
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
