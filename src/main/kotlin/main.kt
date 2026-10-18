package com.example

import com.example.database.configureDatabases
import com.example.plagins.configureRouting
import com.example.plagins.configureSecurity
import com.example.plagins.configureSerialization
import com.example.plagins.configureStatusPages
import com.example.plagins.jwtSettings
import com.example.repository.EquipmentRepositoryImpl
import com.example.repository.UserRepositoryImpl
import com.example.service.AuthService
import com.example.service.EquipmentService
import com.example.service.JwtService
import com.example.service.UserService
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val jwt = jwtSettings()

    configureDatabases()
    configureSerialization()
    configureSecurity(jwt)
    configureStatusPages()

    val userRepository = UserRepositoryImpl()
    val equipmentRepository = EquipmentRepositoryImpl()

    val authService = AuthService(userRepository)
    val userService = UserService(userRepository, authService)
    val equipmentService = EquipmentService(equipmentRepository)
    val jwtService = JwtService(jwt.secret, jwt.issuer)

    configureRouting(userService, jwtService, equipmentService)
}
