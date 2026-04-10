package com.ttfeed.routes

import com.ttfeed.service.MatchService
import com.ttfeed.service.StandingsService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.groupRoutes() {
    route("/groups") {
        get("/{id}/standings") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing group id")
            val standings = StandingsService.getForGroup(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Group not found")
            call.respond(standings)
        }
        get("/{id}/matches") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing group id")
            val matches = MatchService.getForGroup(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Group not found")
            call.respond(matches)
        }
    }
}