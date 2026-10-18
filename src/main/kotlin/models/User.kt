package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val email: String,
    val passwordHash:String,
    val role: String)
// DTO для регистрации и входа
@Serializable
data class AuthRequest(
    val email: String,
    val plainPassword: String,
    val role: String
)
@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    val role: String
)
// DTO для ответа с токеном
@Serializable
data class AuthResponse(
    val token: String
)
