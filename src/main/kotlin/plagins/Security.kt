import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity(){
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()

    install(Authentication){
        jwt("auth-jwt"){ // Имя схемы
            realm = "LabTech Server"

            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer) // Должно совпадать с тем, что ставишь в JwtService
                    .build()
            )

            validate { credential ->
                // Проверяем, что в токене есть Subject (это наш email)
                if (credential.payload.subject != null && credential.payload.subject.isNotEmpty()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}