package com.example.repository

import UserRepository
import UsersTable
import com.example.Exception.DuplicateResourceException
import com.example.Exception.InternalServerException
import com.example.models.User
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserRepositoryImpl : UserRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction { block() }

    private fun toUser(row: ResultRow): User = User(
        id = row[UsersTable.id],
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        role = row[UsersTable.role]
    )


    override suspend fun create(email: String, passwordHash: String, role: String): User = dbQuery {
        try {
            UsersTable.insert {
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.role] = role
            }.let { stmt ->
                toUser(stmt.resultedValues!!.first())
            }
        } catch (e: ExposedSQLException) { // Или SQLException, зависит от драйвера
            if (e.sqlState == "23505") {
                throw DuplicateResourceException("Почта '$email' уже зарегистрирована")
            }
            throw InternalServerException("Database error during creation", e)
        }
    }

    override suspend fun getAll(): List<User> = dbQuery {
        UsersTable.selectAll().map(::toUser).toList()
    }

    override suspend fun getHash(email: String): String? = dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .map { toUser(it) }
            .singleOrNull()?.passwordHash
    }


    override suspend fun findByEmail(email: String): User? = dbQuery {
        try {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .map(::toUser)
                .singleOrNull()
        } catch (e: ExposedSQLException) { // Или SQLException, зависит от драйвера
            if (e.sqlState == "23505 ") {
                throw DuplicateResourceException("Почта '$email' уже зарегистрирована")
            }
            throw InternalServerException("Database error during creation", e)
        }
    }

    override suspend fun findById(id: Int): User? = dbQuery {
        UsersTable
            .selectAll()
            .where(UsersTable.id eq id)
            .map(::toUser)
            .singleOrNull()
    }

    override suspend fun remove(email: String) {
        UsersTable
            .deleteWhere { UsersTable.email eq email }
    }
    override suspend fun remove(id: Int) {
        UsersTable
            .deleteWhere { UsersTable.id eq id }
    }

    
}
