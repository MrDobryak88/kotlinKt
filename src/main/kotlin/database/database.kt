package com.example.database

import EquipmentTable
import UsersTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val config = environment.config

    Database.connect(
        url = config.property("db.url").getString(),
        driver = config.propertyOrNull("db.driver")?.getString() ?: "org.postgresql.Driver",
        user = config.property("db.user").getString(),
        password = config.property("db.password").getString()
    )

    transaction {
        SchemaUtils.create(UsersTable, EquipmentTable)
    }
}
