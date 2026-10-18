package com.example.plagins

import com.example.Exception.AppException
import com.example.models.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        // Наши прикладные ошибки (400/401/403/404/409/500)
        exception<AppException> { call, cause ->
            if (cause.statusCode >= 500) {
                call.application.log.error("Internal error", cause)
            }
            call.respond(
                HttpStatusCode.fromValue(cause.statusCode),
                ErrorResponse(status = cause.statusCode, error = cause.message ?: "")
            )
        }

        // Некорректный JSON / не хватает обязательных полей в теле запроса
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(status = 400, error = "Invalid request body")
            )
        }

        exception<SerializationException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(status = 400, error = "Invalid request body")
            )
        }

        // Всё остальное: логируем и отдаём общий ответ без подробностей
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(status = 500, error = "Server Error")
            )
        }
    }
}
