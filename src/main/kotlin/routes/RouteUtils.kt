package com.example.routes

import com.example.Exception.ForbiddenException
import com.example.Exception.UnauthorizedException
import com.example.Exception.UserNotFoundException
import com.example.Exception.ValidationException
import com.example.models.User
import com.example.service.UserService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

/** Пользователь, которому принадлежит JWT-токен текущего запроса. */
suspend fun ApplicationCall.currentUser(userService: UserService): User {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("No authentication token provided")
    val email = principal.payload.subject
        ?: throw UnauthorizedException("Invalid token")

    return try {
        userService.findByEmail(email)
    } catch (e: UserNotFoundException) {
        // Токен ещё действует, но пользователя уже удалили
        throw UnauthorizedException("User no longer exists")
    }
}

/** То же, что currentUser, но дополнительно требует роль ADMIN. */
suspend fun ApplicationCall.requireAdmin(userService: UserService): User {
    val user = currentUser(userService)
    if (!user.role.equals(UserService.ROLE_ADMIN, ignoreCase = true)) {
        throw ForbiddenException("Access denied. Admin privileges required.")
    }
    return user
}

/** Числовой параметр из пути, например /{id}. */
fun ApplicationCall.pathId(name: String = "id"): Int =
    parameters[name]?.toIntOrNull() ?: throw ValidationException("Invalid $name format")
