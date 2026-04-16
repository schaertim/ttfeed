package com.ttfeed.routes

import com.ttfeed.service.TeamService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.teamRoutes() {
    route("/teams") {
        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing team id")
            val summary = TeamService.getTeamSummary(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Team not found")
            call.respond(summary)
        }

        get("/{id}/roster") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing team id")
            val roster = TeamService.getTeamRoster(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Team not found")
            call.respond(roster)
        }

        get("/{id}/matches") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing team id")
            val matches = TeamService.getTeamMatches(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Team not found")
            call.respond(matches)
        }
    }
}
