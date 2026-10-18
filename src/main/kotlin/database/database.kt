package com.example.database

import EquipmentTable
import UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.application.Application

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/lab_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "54368"
    )

    transaction {
        SchemaUtils.create(UsersTable, EquipmentTable)
    }
}