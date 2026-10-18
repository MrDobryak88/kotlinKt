package com.example.routes

import com.example.Exception.ValidationException
import com.example.models.EquipmentRequest
import com.example.plagins.JWT_AUTH
import com.example.service.EquipmentService
import com.example.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

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
                val request = call.receive<EquipmentRequest>()
                call.respond(HttpStatusCode.Created, equipmentService.create(request))
            }

            put("/{id}") {
                call.requireAdmin(userService)
                val id = call.pathId("id")
                val request = call.receive<EquipmentRequest>()
                call.respond(equipmentService.update(id, request))
            }

            delete("/{id}") {
                call.requireAdmin(userService)
                equipmentService.delete(call.pathId("id"))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
