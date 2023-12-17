package com.nabiha.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class GitHubWebhookPayload(val action: String)

fun Application.githubWebhook() {
    routing {
        post("/github-webhook") {
            val payload = call.receive<GitHubWebhookPayload>()
            val signature = call.request.headers["X-Hub-Signature"]
            val secret = "Api Kontlin Nih"

            if (signature != null && isValidGitHubWebhook(payload.action, signature, secret)) {
                val pullResult = runGitPull("C:\\Projek Android\\Bwave-Kotlin-Api")
                call.respondText("Git Pull Result: $pullResult", status = HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
            }
        }
    }
}

fun runGitPull(repositoryPath: String): String {
    val command = "git pull"
    return try {
        val processBuilder = ProcessBuilder("cmd.exe", "/c", command)
            .directory(java.io.File(repositoryPath))
            .redirectErrorStream(true)

        val process = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append('\n')
        }

        val exitCode = process.waitFor()
        output.append("Exit Code: $exitCode")

        output.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${e.message}"
    }
}

fun isValidGitHubWebhook(payload: String, signature: String, secret: String): Boolean {
    val calculatedSignature = calculateHMAC(payload, secret)
    return calculatedSignature == signature
}

fun calculateHMAC(data: String, secret: String): String {
    val algorithm = "HmacSHA256"
    val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(keySpec)
    val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    return "sha256=" + Base64.getEncoder().encodeToString(rawHmac)
}
