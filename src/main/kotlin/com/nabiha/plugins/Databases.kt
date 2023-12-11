package com.nabiha.plugins

import com.nabiha.AppConfig
import com.nabiha.plugins.controller.auth.authController
import com.nabiha.plugins.controller.companies.companiesController
import com.nabiha.plugins.controller.users.usersController
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*

fun Application.configureDatabases() {
    val database = Database.connect(
        url = AppConfig.postgres.url,
        user = AppConfig.postgres.user,
        driver = AppConfig.postgres.driver,
        password = AppConfig.postgres.password
    )
    usersController(database)
    companiesController(database)
    authController(database)
}
