package com.example.plagins

import com.example.routes.configureAuthRoutes
import com.example.routes.equipmentRoutes
import com.example.routes.userRoutes
import com.example.service.EquipmentService
import com.example.service.JwtService
import com.example.service.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(
    userService: UserService,
    jwtService: JwtService,
    equipmentService: EquipmentService
) {
    routing {
        // Проверка, что сервер жив
        get("/") {
            call.respondText("LabTech Server is running")
        }

        // Страница для ручной проверки API: http://localhost:8080/app
        staticResources("/app", "web")

        configureAuthRoutes(userService, jwtService)
        userRoutes(userService)
        equipmentRoutes(userService, equipmentService)
    }
}
