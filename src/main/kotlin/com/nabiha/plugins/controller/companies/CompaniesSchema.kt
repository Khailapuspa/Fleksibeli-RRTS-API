package com.nabiha.plugins.controller.companies

import com.nabiha.dateNow
import com.nabiha.plugins.controller.users.UserService.Users.default
import com.nabiha.plugins.controller.users.UserService.Users.nullable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedCompany(var name: String, var logo: String)
class CompanyService(private val database: Database) {
    object Companies : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val logo = varchar("logo", length = 255).default("")
        val at_create = varchar("at_create", length = 35).nullable()
        val at_update = varchar("at_update", length = 35).nullable()
        val at_flag = integer("at_flag").default(1).nullable()
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Companies)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(company: ExposedCompany): Int = dbQuery {
        Companies.insert {
            it[name] = company.name
            it[logo] = company.logo
            it[at_create] = dateNow()
            it[at_update] = dateNow()
        }[Companies.id]
    }

    suspend fun reads(): List<ExposedCompany> {
        return dbQuery {
            Companies.selectAll()
                .map { ExposedCompany(it[Companies.name], it[Companies.logo]) }

        }
    }

    suspend fun read(id: Int): ExposedCompany? {
        return dbQuery {
            Companies.select { Companies.id eq id }
                .map { ExposedCompany(it[Companies.name], it[Companies.logo]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, company: ExposedCompany) {
        dbQuery {
            Companies.update({ Companies.id eq id }) {
                it[name] = company.name
                it[logo] = company.logo
                it[at_update] = dateNow()
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Companies.deleteWhere { Companies.id.eq(id) }
        }
    }

}