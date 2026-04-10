package com.ttfeed.routes

import com.ttfeed.service.PlayerService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            call.respond(player)
        }
    }
}