package com.example.plagins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Duration

/** Рассылка уведомлений всем подключённым WebSocket-клиентам. */
object NotificationHub {
    private val flow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events = flow.asSharedFlow()
    fun send(type: String, message: String) {
        flow.tryEmit("""{"type":"$type","message":"$message"}""")
    }
}

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
    }
    routing {
        webSocket("/ws/notifications") {
            send(Frame.Text("""{"type":"CONNECTED","message":"Notifications on"}"""))
            val job = NotificationHub.events.onEach { send(Frame.Text(it)) }.launchIn(this)
            try {
                for (frame in incoming) { /* клиент ничего не отправляет */ }
            } finally {
                job.cancel()
            }
        }
    }
}