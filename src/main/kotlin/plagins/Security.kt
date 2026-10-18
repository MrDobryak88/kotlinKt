package com.example.plagins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

/** Имя схемы аутентификации. Используется в authenticate(...) во всех роутах. */
const val JWT_AUTH = "auth-jwt"

data class JwtSettings(
    val secret: String,
    val issuer: String,
    val realm: String
)

/** Читает настройки JWT из application.yaml (секрет можно задать через JWT_SECRET). */
fun Application.jwtSettings(): JwtSettings {
    val config = environment.config
    return JwtSettings(
        secret = config.property("jwt.secret").getString(),
        issuer = config.property("jwt.issuer").getString(),
        realm = config.propertyOrNull("jwt.realm")?.getString() ?: "LabTech Server"
    )
}

fun Application.configureSecurity(settings: JwtSettings) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = settings.realm

            verifier(
                JWT.require(Algorithm.HMAC256(settings.secret))
                    .withIssuer(settings.issuer) // Должно совпадать с тем, что ставит JwtService
                    .build()
            )

            validate { credential ->
                // В токене должен быть subject (это email пользователя)
                val subject = credential.payload.subject
                if (!subject.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
