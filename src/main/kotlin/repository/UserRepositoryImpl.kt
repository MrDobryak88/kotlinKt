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
        } catch (e: ExposedSQLException) {
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
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .map(::toUser)
            .singleOrNull()
    }

    override suspend fun findById(id: Int): User? = dbQuery {
        UsersTable
            .selectAll()
            .where(UsersTable.id eq id)
            .map(::toUser)
            .singleOrNull()
    }

    // Удаление обязано выполняться внутри транзакции, иначе Exposed бросает исключение
    override suspend fun remove(email: String) {
        val targetEmail = email // локальная копия, чтобы не спутать с колонкой UsersTable.email
        dbQuery {
            UsersTable.deleteWhere { UsersTable.email eq targetEmail }
        }
    }

    override suspend fun remove(id: Int) {
        val targetId = id
        dbQuery {
            UsersTable.deleteWhere { UsersTable.id eq targetId }
        }
    }

    override suspend fun hasUsers(): Boolean = dbQuery {
        UsersTable.selectAll().count() > 0
    }
}
