package com.example.routes

import com.example.models.toResponse
import com.example.plagins.JWT_AUTH
import com.example.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.userRoutes(userService: UserService) {

    route("/api") {
        authenticate(JWT_AUTH) {

            // Профиль текущего пользователя
            get("/me") {
                val user = call.currentUser(userService)
                call.respond(user.toResponse())
            }

            // Список всех пользователей (только ADMIN)
            get("/users") {
                call.requireAdmin(userService)
                call.respond(userService.getAll().map { it.toResponse() })
            }

            // Удаление пользователя (только ADMIN, себя удалить нельзя)
            post("/admin/delete-user/{id}") {
                val admin = call.requireAdmin(userService)
                val targetUserId = call.pathId("id")

                userService.deleteUser(targetUserId, requesterEmail = admin.email)

                call.respond(HttpStatusCode.OK, mapOf("message" to "User $targetUserId deleted successfully"))
            }
        }
    }
}
