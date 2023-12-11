package com.nabiha.plugins.controller.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nabiha.AppConfig
import com.nabiha.EmailConf
import com.nabiha.generateRandomString
import com.nabiha.plugins.controller.companies.CompanyService
import com.nabiha.plugins.controller.companies.ExposedCompany
import com.nabiha.plugins.controller.users.CreateUserResponse
import com.nabiha.plugins.controller.users.ErrorRespond
import com.nabiha.plugins.controller.users.ExposedUser
import com.nabiha.plugins.controller.users.UserService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.lang.Exception
import java.util.*

@Serializable
data class AuthResponse(
    val success: Boolean = true,
    val data: ExposedUser? = null,
)

@Serializable
data class SendEmailRequest(
    val email: String,
)
@Serializable
data class VerificationAccount(
    val code: String,
)

fun Application.authController(database: Database) {

    val authServices = AuthServices(database)
    val userService = UserService(database)
    val companyServices = CompanyService(database)

    routing {
        post("/register/user") {
            try {
                val body = call.receive<ExposedRegisterUser>()
                val company = ExposedCompany(body.name, "")
                val companyId = companyServices.create(company)
                val user = ExposedUser(body.name, body.email, body.age, companyId, 3)
                val userCredential = ExposedUserCredential(body.password)
                val id = userService.create(user)
                authServices.create(userCredential, id)
                val data = userService.read(id)
                call.respond(HttpStatusCode.Created, CreateUserResponse(data = data))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }

        post("/register/company") {
            try {
                val multipart = call.receiveMultipart()
                val company = ExposedRegisterCompany("","",0,"","","")

                multipart.forEachPart { part ->
                    if (part.name == "name" && part is PartData.FormItem) {
                        company.name = part.value
                    }
                    if (part.name == "email" && part is PartData.FormItem) {
                        company.email = part.value
                    }
                    if (part.name == "age" && part is PartData.FormItem) {
                        company.age = part.value.toInt()
                    }
                    if (part.name == "password" && part is PartData.FormItem) {
                        company.password = part.value
                    }
                    if (part.name == "companyName" && part is PartData.FormItem) {
                        company.companyName = part.value
                    }

                    if (part.name == "companyLogo" && part is PartData.FileItem) {
                        // retrieve file name of upload
                        val originalFileName = part.originalFileName ?: "file-${UUID.randomUUID()}"
                        val fileExtension = originalFileName.substringAfterLast('.', "")
                        val name = "logo-${UUID.randomUUID()}.$fileExtension"
                        val file = File("src/main/resources/logos-company/$name")

                        part.streamProvider().use { its ->
                            // copy the stream to the file with buffering
                            file.outputStream().buffered().use {
                                // note that this is blocking
                                its.copyTo(it)
                            }
                        }

                        company.companyLogo = name
                    }
                    part.dispose()
                }
                val companyCreate = ExposedCompany(company.companyName, company.companyLogo)
                val companyId = companyServices.create(companyCreate)
                val user = ExposedUser(company.name, company.email, company.age, companyId, 2)
                val userCredential = ExposedUserCredential(company.password)
                val id = userService.create(user)
                authServices.create(userCredential, id)
                val data = userService.read(id)
                call.respond(HttpStatusCode.Created, CreateUserResponse(data = data))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }

        post("/login") {
            try {
                val body = call.receive<ExposedLogin>()
                val userId = userService.getId(body.email)
                if (userId != null) {
                    val checkPassword = authServices.checkPassword(body.password, userId)
                    if (checkPassword) {
                        val token = JWT.create()
                            .withAudience(AppConfig.jwt.jwtAudience)
                            .withIssuer(AppConfig.jwt.jwtDomain)
                            .withClaim("userId", userId)
                            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                            .sign(Algorithm.HMAC256(AppConfig.jwt.jwtSecret))
                        call.respond(hashMapOf("token" to token))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorRespond(message = "Wrong Password!"))
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorRespond(message = "User not Found!"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }

        authenticate("auth-jwt") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val user = userService.read(userId)
                call.respond(HttpStatusCode.OK, AuthResponse(data = user))
            }

            post("/email/request") {
                try {
                    val emailUser = call.receive<SendEmailRequest>().email
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("userId").asInt()
                    val randomString = generateRandomString(6)
                    val send = EmailConf.sendEmail(emailUser, randomString)
                    if (send) {
                        authServices.verifyRequest(randomString, userId)
                        call.respond(HttpStatusCode.OK, "email Send!")
                    } else {
                        call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = "Email Not Send!"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
                }
            }
            post("/email/verification") {
                try {
                    val body = call.receive<VerificationAccount>()
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("userId").asInt()
                    val checkCode = authServices.verifyAccount(body.code,userId)
                    if (checkCode){
                        call.respond(HttpStatusCode.OK, "User Verified!")
                    }else{
                        call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = "Code not match!"))
                    }
                }catch (e: Exception) {
                    call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
                }
            }
        }

    }
}