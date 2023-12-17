package com.nabiha

import com.nabiha.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val env = applicationEngineEnvironment {
        envConfig()
    }
    embeddedServer(Netty, env).start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
}

fun ApplicationEngineEnvironmentBuilder.envConfig() {
    module {
        module()
    }
    connector {
        host = AppConfig.connector.host
        port = AppConfig.connector.port
    }
}
