package com.nabiha.plugins.controller.users

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import java.lang.Exception

@Serializable
data class CreateUserResponse(
    val success: Boolean = true,
    val data: ExposedUser? = null,
    val datas: List<ExposedUser>? = null
)

@Serializable
data class ErrorRespond(val success: Boolean = false, val message: String? = null)

fun Application.usersController(database: Database) {
    val userService = UserService(database)
    routing {
        // Read Users
        get("/users-users") {
            try {
                val users = userService.reads()
                call.respond(HttpStatusCode.OK, CreateUserResponse(datas = users))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }
        // Read Users
        get("/users") {
            try {
                val users = userService.reads()
                call.respond(HttpStatusCode.OK, CreateUserResponse(datas = users))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }
        // Read user
        get("/users/{id}") {
            try {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val user = userService.read(id)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, CreateUserResponse(data = user))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorRespond(message = "User Not Found!"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }
        // Update user
        put("/users/{id}") {
            try {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val user = call.receive<ExposedUser>()
                userService.update(id, user)
                val data = userService.read(id)
                call.respond(HttpStatusCode.OK, CreateUserResponse(data = data))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }
        // Delete user
        delete("/users/{id}") {
            try {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                userService.delete(id)
                call.respond(HttpStatusCode.OK, CreateUserResponse())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }
    }
}