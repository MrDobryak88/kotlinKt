package com.example.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JwtService(private val secretKey: String, private val issuer: String) {
    fun generateToken(email: String): String {
        val algorithm = Algorithm.HMAC256(secretKey)

        return JWT.create()
            .withIssuer(issuer) // Используем параметр, а не хардкод
            .withSubject(email)   // Email идет сюда
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)
    }

    // Метод для валидации токена (пригодится позже в Security.kt)
    fun validateToken(token: String): String? {
        try {
            val algorithm = Algorithm.HMAC256(secretKey)
            val verifier = JWT.require(algorithm)
                .withIssuer("my-app")
                .build()

            val decodedJWT = verifier.verify(token)
            return decodedJWT.subject // Возвращаем email, если токен валиден
        } catch (e: Exception) {
            return null // Токен протух или подделан
        }
    }
}