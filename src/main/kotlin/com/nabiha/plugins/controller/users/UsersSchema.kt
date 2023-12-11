package com.nabiha.plugins.controller.users

import com.nabiha.dateNow
import com.nabiha.plugins.controller.companies.CompanyService
import com.nabiha.plugins.controller.users.UserService.Users.autoIncrement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import java.util.*

@Serializable
data class ExposedUser(val name: String, val email: String, val age: Int, val companyId: Int, val level:Int)

class UserService(private val database: Database) {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val email = varchar("email", length = 255).uniqueIndex()
        val name = varchar("name", length = 50)
        val age = integer("age")
        val level = integer("level")
        val companyId = integer("companyId").references(CompanyService.Companies.id)
        val at_create = varchar("at_create", length = 35).nullable()
        val at_update = varchar("at_update", length = 35).nullable()
        val at_flag = integer("at_flag").default(0).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(user: ExposedUser): Int = dbQuery {
        Users.insert {
            it[name] = user.name
            it[age] = user.age
            it[email] = user.email
            it[level] = user.level
            it[companyId] = user.companyId
            it[at_create] = dateNow()
            it[at_update] = dateNow()
        }[Users.id]
    }

    suspend fun reads(): List<ExposedUser> {
        return dbQuery {
            Users.selectAll()
                .map {
                    ExposedUser(
                        email = it[Users.email],
                        name = it[Users.name],
                        age = it[Users.age],
                        level = it[Users.level],
                        companyId = it[Users.companyId]
                    )
                }

        }
    }

    suspend fun read(id: Int): ExposedUser? {
        return dbQuery {
            Users.select { Users.id eq id }
                .map {
                    ExposedUser(
                        email = it[Users.email],
                        name = it[Users.name],
                        age = it[Users.age],
                        level = it[Users.level],
                        companyId = it[Users.companyId]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun getId(email: String): Int? {
        return dbQuery {
            Users.select { Users.email eq email }.singleOrNull()?.get(Users.id)
        }
    }

    suspend fun update(id: Int, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[name] = user.name
                it[age] = user.age
                it[at_update] = dateNow()
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }
}
