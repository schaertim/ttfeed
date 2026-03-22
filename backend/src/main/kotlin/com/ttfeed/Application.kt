package com.ttfeed

import com.ttfeed.com.ttfeed.database.configureDatabase
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
}