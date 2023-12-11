package com.nabiha.plugins.controller.auth


import com.nabiha.plugins.controller.users.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt


@Serializable
data class ExposedUserCredential(val password: String)

@Serializable
data class ExposedVerification(val code: String)

@Serializable
data class ExposedRegisterUser(
    val name: String,
    val email: String,
    val age: Int,
    val password: String,
)

@Serializable
data class ExposedRegisterCompany(
    var name: String,
    var email: String,
    var age: Int,
    var password: String,
    var companyName: String,
    var companyLogo: String
)

@Serializable
data class ExposedLogin(val email: String, val password: String)

class AuthServices(private val database: Database) {

    object UsersCredential : Table() {
        val id = integer("id").autoIncrement()
        val password = varchar("password", length = 255)
        val userId = integer("userId").references(UserService.Users.id)
    }

    object UsersVerification : Table() {
        val id = integer("id").autoIncrement()
        val code = varchar("code", length = 50)
        val userId = integer("userId").references(UserService.Users.id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(UsersCredential)
            SchemaUtils.create(UsersVerification)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(credential: ExposedUserCredential, id: Int): Int = dbQuery {
        val hashedPassword = BCrypt.hashpw(credential.password, BCrypt.gensalt())
        UsersCredential.insert {
            it[password] = hashedPassword
            it[userId] = id
        }[UsersCredential.id]
    }

    suspend fun verifyRequest(codeVerify: String, id: Int) {
        val check = dbQuery {
            UsersVerification.select { UsersVerification.userId eq id }.singleOrNull()
        }
        if (check == null) {
            dbQuery {
                UsersVerification.insert {
                    it[code] = codeVerify
                    it[userId] = id
                }[UsersVerification.id]
            }
        } else {
            dbQuery {
                UsersVerification.update({ UsersVerification.userId eq id }) {
                    it[code] = codeVerify
                }
            }
        }

    }

    suspend fun verifyAccount(codeVerify: String, id: Int): Boolean {
        val codeStored = dbQuery {
            UsersVerification.select { UsersVerification.userId eq id }.single()[UsersVerification.code]
        }
        return if (codeStored == codeVerify) {
            dbQuery {
                UserService.Users.update({ UserService.Users.id eq id }) {
                    it[at_flag] = 1
                }
            }
            true
        } else {
            false
        }
    }

    suspend fun checkPassword(password: String, id: Int): Boolean {
        val binaryPassword =
            dbQuery { UsersCredential.select { UsersCredential.id eq id }.single()[UsersCredential.password] }

        return BCrypt.checkpw(password, binaryPassword)
    }

}