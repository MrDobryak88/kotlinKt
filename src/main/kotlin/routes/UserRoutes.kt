package com.example.routes

import com.example.Exception.ForbiddenException // Убедись, что этот импорт есть
import com.example.Exception.UnauthorizedException
import com.example.Exception.ValidationException
import com.example.models.UserResponse
import com.example.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Передаем сервис в функцию
fun Route.userRoutes(userService: UserService) {

    route("/api") {
        authenticate("jwt-auth") { // Схема должна называться точно так же, как в Security.kt

            get("/me") {
                // 1. Получаем данные из токена
                val principal = call.principal<JWTPrincipal>()
                    ?: throw ForbiddenException("No principal found in context") // Защита от null

                val email = principal.payload.subject

                val user = userService.findByEmail(email) // Предполагаю, что этот метод есть и возвращает User? или User


                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User profile not found"))
                } else {
                    val responseDto = UserResponse(
                        id = user.id,
                        email = user.email,
                        role = user.role
                    )
                    call.respond(responseDto)
                }
            }

            post("/admin/delete-user/{id}") {
                // 1. Получаем email текущего пользователя из токена
                val principal = call.principal<JWTPrincipal>()
                    ?: throw UnauthorizedException("No authentication token provided")

                val adminEmail = principal.payload.subject

                // 2. Находим этого пользователя в БД, чтобы узнать его актуальную роль
                val adminUser = userService.findByEmail(adminEmail)

                // 3. Проверяем права доступа (RBAC)
                if (adminUser.role.uppercase() != "ADMIN") {
                    throw ForbiddenException("Access denied. Admin privileges required.")
                }

                // 4. Парсим ID из пути URL
                val targetUserId = call.parameters["id"]?.toIntOrNull()
                    ?: throw ValidationException("Invalid user ID format")

                // 5. Вызываем сервис удаления
                // Передаем email админа, чтобы сервис мог проверить запрет самоудаления
                userService.deleteUser(targetUserId, requesterEmail = adminEmail)

                // 6. Ответ об успехе
                call.respond(HttpStatusCode.OK, mapOf("message" to "User $targetUserId deleted successfully"))
            }
        }
    }
}