package com.nabiha.plugins.controller.companies

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.lang.Exception
import java.util.*

@Serializable
data class CreateCompanyResponse(
    val success: Boolean = true,
    val data: ExposedCompany? = null,
    val datas: List<ExposedCompany>? = null
)

@Serializable
data class ErrorRespond(val success: Boolean = false, val message: String? = null)

fun Application.companiesController(database: Database) {
    val companyService = CompanyService(database)
    routing {
        post("/companies") {
            try {
                val multipart = call.receiveMultipart()
                var companyName = ""
                var logoUrl = ""
                val uploadDirectory = File("src/main/resources/logos-company")
                if (!uploadDirectory.exists()) {
                    uploadDirectory.mkdirs()
                }
                multipart.forEachPart { part ->
                    if (part.name == "name" && part is PartData.FormItem) {
                        companyName = part.value
                    }

                    if (part.name == "logo" && part is PartData.FileItem) {
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

                        logoUrl = "logos-company/$name"

                    }
                    part.dispose()
                }
                val company = ExposedCompany(companyName, logoUrl)
                val id = companyService.create(company)
                val data = companyService.read(id)
                call.respond(HttpStatusCode.Created, CreateCompanyResponse(data = data))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, ErrorRespond(message = e.message))
            }
        }
        get("/companies") {
            try {
                val companies = companyService.reads()
                call.respond(HttpStatusCode.OK, CreateCompanyResponse(datas = companies))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorRespond(message = e.message)
                )
            }
        }
        get("companies/logo") {
            try {
                val logo = call.parameters["name"]!!
                val file = File("src/main/resources/logos-company/$logo")
                if(file.exists()) {
                    call.respondFile(file)
                }
                else call.respond(HttpStatusCode.NotFound)
            }catch (e: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorRespond(message = e.message)
                )
            }

        }

        get("/company") {
            try {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val company = companyService.read(id)
                if (company != null) {
                    call.respond(HttpStatusCode.OK, CreateCompanyResponse(data = company))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorRespond(message = "company Not Found!")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorRespond(message = e.message)
                )
            }
        }

        put("/company") {
            try {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val company = call.receive<ExposedCompany>()
                companyService.update(id, company)
                val data = companyService.read(id)
                call.respond(HttpStatusCode.OK, CreateCompanyResponse(data = data))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorRespond(message = e.message)
                )
            }
        }

        delete("/company") {
            try {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                companyService.delete(id)
                call.respond(HttpStatusCode.OK, CreateCompanyResponse())
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorRespond(message = e.message)
                )
            }
        }
    }
}