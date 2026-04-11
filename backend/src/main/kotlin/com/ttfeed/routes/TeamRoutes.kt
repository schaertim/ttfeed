package com.ttfeed.routes

import com.ttfeed.service.TeamService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.teamRoutes() {
    route("/teams") {
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val summary = TeamService.getTeamSummary(id)
            if (summary != null) call.respond(summary) else call.respond(HttpStatusCode.NotFound)
        }

        get("/{id}/roster") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val roster = TeamService.getTeamRoster(id)
            if (roster != null) call.respond(roster) else call.respond(HttpStatusCode.NotFound)
        }

        get("/{id}/matches") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val matches = TeamService.getTeamMatches(id)
            if (matches != null) call.respond(matches) else call.respond(HttpStatusCode.NotFound)
        }
    }
}