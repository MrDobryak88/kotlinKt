package com.example.routes

import com.example.Exception.UnauthorizedException
import com.example.models.AuthRequest // DTO для регистрации
import com.example.models.SuccessResponse
import com.example.service.AuthService
import com.example.service.JwtService
import com.example.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.configureAuthRoutes(
    authService: AuthService,
    userService: UserService,
    jwtService: JwtService // Передаем сервис токенов
) {

    get("/users") {
        // TODO: Реализовать получение списка пользователей
    }


    post("/register") {
        val request = call.receive<AuthRequest>()
        // Создаем юзера через сервис (там уже есть валидация и хеширование)
        val user = userService.create(request.email, request.plainPassword, request.role)

        call.respond(HttpStatusCode.Created, SuccessResponse(message = "Пользователь создан", data = user))
    }

    post("/login") {
        try {
            val request = call.receive<AuthRequest>() // DTO с email и password

            // Вызываем НОВЫЙ метод сервиса
            val user = userService.login(request.email, request.plainPassword)

            // Если дошли сюда — пароль верный. Генерируем токен.
            val token = jwtService.generateToken(user.email)

            call.respond(mapOf("token" to token))

        } catch (e: UnauthorizedException) {
            // StatusPages перехватит это и вернет 401
            throw e
        }
    }
}