package com.example

import com.example.Exception.AppException // Убедись, что пакет называется именно так (с большой буквы?)
import com.example.database.configureDatabases
import com.example.models.ErrorResponse
import com.example.repository.UserRepositoryImpl
import com.example.routes.configureAuthRoutes
import com.example.routes.userRoutes
import com.example.service.AuthService
import com.example.service.JwtService
import com.example.service.UserService
import configureSerialization
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.StatusPages // !!! ВАЖНО: Импорт плагина
import io.ktor.server.response.respond // !!! ВАЖНО: Для call.respond
import io.ktor.http.HttpStatusCode      // !!! ВАЖНО: Для кодов статуса
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabases()
    configureSerialization()
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString() // Читаем из конфига

    val userRepository = UserRepositoryImpl()
    val authService = AuthService(userRepository)
    val userService = UserService(userRepository, authService)
    val jwtService = JwtService(jwtSecret, jwtIssuer)

    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.statusCode),
                ErrorResponse(status = cause.statusCode, error = cause.message ?: "")
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server Error"))
        }
    }


    routing {
        configureAuthRoutes(authService, userService, jwtService)

            userRoutes(userService)
    }
}