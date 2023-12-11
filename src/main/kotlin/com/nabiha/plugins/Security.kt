package com.nabiha.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nabiha.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {

    authentication {

        jwt("auth-jwt") {
            realm = AppConfig.jwt.jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(AppConfig.jwt.jwtSecret))
                    .withAudience(AppConfig.jwt.jwtAudience)
                    .withIssuer(AppConfig.jwt.jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }

    }

}
