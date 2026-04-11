package com.ttfeed

import com.ttfeed.database.configureDatabase
import com.ttfeed.plugins.configureCors
import com.ttfeed.plugins.configureRouting
import com.ttfeed.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
    configureCors()
}