package com.nabiha

object AppConfig {
    object postgres {
        const val url = "jdbc:postgresql://localhost:5432/bwave_database"
        const val user = "postgres"
        const val driver = "org.h2.Driver"
        const val password = "yodha"
    }

    object connector {
        const val host = "0.0.0.0"
        const val port = 8080
    }

    object jwt{
        const val jwtAudience = "jwt-audience"
        const val jwtDomain = "https://jwt-provider-domain/"
        const val jwtRealm = "ktor sample app"
        const val jwtSecret = "secret"
    }

    object email{
        const val GOOGLE_SMTP_HOST = "smtp.gmail.com"
        const val smtp_port = "587"
        const val smtp_auth = "true"
        const val smtp_starttls_enable = "true"
        const val emailUsername = "myanonim96@gmail.com"
        const val emailPassword = "iioy eisk jojo fzqc"
        const val fromEmail = "myanonim96@gmail.com"
    }
}