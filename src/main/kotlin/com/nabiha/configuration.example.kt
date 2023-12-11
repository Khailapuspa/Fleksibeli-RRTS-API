package com.nabiha

object AppConfigExample {
    object postgres {
        const val url = ""
        const val user = ""
        const val driver = ""
        const val password = ""
    }

    object connector {
        const val host = "0.0.0.0"
        const val port = 8080
    }

    object jwt{
        const val jwtAudience = ""
        const val jwtDomain = ""
        const val jwtRealm = ""
        const val jwtSecret = ""
    }

    object email{
        const val GOOGLE_SMTP_HOST = "smtp.gmail.com"
        const val smtp_port = "587"
        const val smtp_auth = "true"
        const val smtp_starttls_enable = "true"
        const val emailUsername = ""
        const val emailPassword = ""
        const val fromEmail = ""
    }
}