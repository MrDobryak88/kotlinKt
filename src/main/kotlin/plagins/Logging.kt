package com.example.plagins

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey

private val StartTime = AttributeKey<Long>("startTime")

val RequestTimingPlugin = createApplicationPlugin("RequestTiming") {
    onCall { call -> call.attributes.put(StartTime, System.currentTimeMillis()) }
    onCallRespond { call, _ ->
        val ms = System.currentTimeMillis() - call.attributes[StartTime]
        call.application.log.info(
            "${call.request.httpMethod.value} ${call.request.path()} -> ${call.response.status()} (${ms} ms)"
        )
    }
}

fun Application.configureLogging() {
    install(CallLogging)
    install(RequestTimingPlugin)
}