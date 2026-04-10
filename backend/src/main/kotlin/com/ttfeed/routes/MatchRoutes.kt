package com.ttfeed.routes

import com.ttfeed.service.MatchService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.matchRoutes() {
    route("/matches") {
        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing match id")
            val matches = MatchService.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Match not found")
            call.respond(matches)
        }
    }
}