package com.example.routes

import com.example.models.AuthRequest
import com.example.models.AuthResponse
import com.example.models.SuccessResponse
import com.example.models.toResponse
import com.example.service.JwtService
import com.example.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.configureAuthRoutes(
    userService: UserService,
    jwtService: JwtService
) {

    post("/register") {
        val request = call.receive<AuthRequest>()
        // Валидация, хеширование пароля и выбор роли делаются в сервисе
        val user = userService.register(request.email, request.plainPassword)

        // Возвращаем DTO без хеша пароля
        call.respond(
            HttpStatusCode.Created,
            SuccessResponse(message = "User created", data = user.toResponse())
        )
    }

    post("/login") {
        val request = call.receive<AuthRequest>()

        // Неверные данные -> UnauthorizedException, StatusPages вернёт 401
        val user = userService.login(request.email, request.plainPassword)

        call.respond(AuthResponse(token = jwtService.generateToken(user.email)))
    }
}
