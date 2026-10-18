package com.example.models


import kotlinx.serialization.Serializable

// Универсальный класс для успешных ответов
@Serializable
data class SuccessResponse<T>(
    val message: String,
    var data: T?,
)

// Класс для ошибок
@Serializable
data class ErrorResponse(
    val status: Int,
    val error: String,
    val timestamp: Long = System.currentTimeMillis()
)