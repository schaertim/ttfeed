package com.ttfeed.routes

import com.ttfeed.service.SeasonService
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.seasonRoutes() {
    route("/seasons") {
        get {
            call.respond(SeasonService.getAll())
        }
    }
}