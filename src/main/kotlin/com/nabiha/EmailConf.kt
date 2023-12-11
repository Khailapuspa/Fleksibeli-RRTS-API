package com.nabiha

import com.sun.mail.util.MailConnectException
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object EmailConf {
    const val GOOGLE_SMTP_HOST = AppConfig.email.GOOGLE_SMTP_HOST

    private val props = Properties().apply {
        this["mail.smtp.host"] = GOOGLE_SMTP_HOST
        this["mail.smtp.port"] = AppConfig.email.smtp_port
        this["mail.smtp.auth"] = AppConfig.email.smtp_auth
        this["mail.smtp.starttls.enable"] = AppConfig.email.smtp_starttls_enable
    }
    private val emailUsername = AppConfig.email.emailUsername
    private val emailPassword = AppConfig.email.emailPassword
    private val fromEmail = AppConfig.email.fromEmail

    private val session: Session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            val username = emailUsername
            val password = emailPassword
            return PasswordAuthentication(username, password)
        }
    })

    suspend fun sendEmail(email: String, codeVerif:String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val message = MimeMessage(session)
            val from = fromEmail
            message.setFrom(InternetAddress(from))
            message.setRecipients(
                Message.RecipientType.TO,
                email.lowercase().trim()
            )
            message.subject = "Send Verification"
            message.sentDate = Date()
            message.setText(codeVerif)
            Transport.send(message)
            true
        } catch (mex: MessagingException) {
            println("send failed, exception: $mex")
            false
        } catch (e: MailConnectException) {
            println("email send failed, exception: $e")
            false
        } catch (e: java.net.ConnectException) {
            println("Connection failed: $e")
            false
        } catch (e: Exception) {
            e.printStackTrace()
            println("Unhandled exception while send email ${e.javaClass.name} from ${e.javaClass.packageName}")
            false
        }
    }
}