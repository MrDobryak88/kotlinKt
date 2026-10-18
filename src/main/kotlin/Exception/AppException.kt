package com.example.Exception

open class AppException(message: String,val statusCode:Int =500): RuntimeException(message)

class ValidationException(message: String) : AppException(message, statusCode = 400)
class UnauthorizedException(message: String = "Токен просрочен") : AppException(message, statusCode = 401)
class ForbiddenException(message: String = "Неподходящая роль пользователя") : AppException(message, statusCode = 403)

// Пользователь не найден -> HTTP 404
class UserNotFoundException(message: String = "User not found") : AppException(message, statusCode = 404)

// Любой другой ресурс не найден (например, оборудование) -> HTTP 404
class ResourceNotFoundException(message: String = "Resource not found") : AppException(message, statusCode = 404)

// Дубликат email / Конфликт ресурсов -> HTTP 409
class DuplicateResourceException(message: String = "Resource already exists") : AppException(message, statusCode = 409)
// Внутренняя ошибка сервера (упала БД, нет сети) -> HTTP 500
class InternalServerException(message: String, cause: Throwable?) : AppException(message, statusCode = 500) {
    init {
        // Сохраняем причину, чтобы залогировать стектрейс позже
        this.initCause(cause)
    }
}