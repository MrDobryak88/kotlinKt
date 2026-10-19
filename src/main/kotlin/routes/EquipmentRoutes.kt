package com.example.routes

import com.example.Exception.ValidationException
import com.example.models.EquipmentRequest
import com.example.plagins.JWT_AUTH
import com.example.plagins.NotificationHub
import com.example.service.EquipmentService
import com.example.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Оборудование лаборатории.
 *  - читать список и отдельные записи может любой авторизованный пользователь;
 *  - создавать, менять и удалять — только ADMIN.
 */
fun Route.equipmentRoutes(userService: UserService, equipmentService: EquipmentService) {

    route("/api/equipment") {
        authenticate(JWT_AUTH) {

            // GET /api/equipment            — всё оборудование
            // GET /api/equipment?available=true — только доступное
            get {
                val availableParam = call.request.queryParameters["available"]
                val onlyAvailable = if (availableParam == null) {
                    false
                } else {
                    availableParam.toBooleanStrictOrNull()
                        ?: throw ValidationException("available must be true or false")
                }
                call.respond(equipmentService.getAll(onlyAvailable))
            }

            get("/{id}") {
                call.respond(equipmentService.getById(call.pathId("id")))
            }

            post {
                call.requireAdmin(userService)
                val created = equipmentService.create(call.receive<EquipmentRequest>())
                NotificationHub.send("EQUIPMENT_CREATED", "Добавлено оборудование")
                call.respond(HttpStatusCode.Created, created)
            }

            put("/{id}") {
                call.requireAdmin(userService)
                val id = call.pathId("id")
                val updated = equipmentService.update(id, call.receive<EquipmentRequest>())
                NotificationHub.send("EQUIPMENT_UPDATED", "Оборудование #$id изменено")
                call.respond(updated)
            }

            delete("/{id}") {
                call.requireAdmin(userService)
                val id = call.pathId("id")
                equipmentService.delete(id)
                NotificationHub.send("EQUIPMENT_DELETED", "Оборудование #$id удалено")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
