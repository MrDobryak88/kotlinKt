package com.example.service

import com.example.Exception.ResourceNotFoundException
import com.example.Exception.ValidationException
import com.example.models.EquipmentRequest
import com.example.models.LabEquipment
import com.example.repository.EquipmentRepository

class EquipmentService(private val repo: EquipmentRepository) {

    suspend fun getAll(onlyAvailable: Boolean = false): List<LabEquipment> =
        repo.getAll(onlyAvailable)

    suspend fun getById(id: Int): LabEquipment =
        repo.findById(id) ?: throw notFound(id)

    suspend fun create(request: EquipmentRequest): LabEquipment {
        val clean = validated(request)
        return repo.create(clean)
    }

    suspend fun update(id: Int, request: EquipmentRequest): LabEquipment {
        val clean = validated(request)
        return repo.update(id, clean) ?: throw notFound(id)
    }

    suspend fun delete(id: Int) {
        if (!repo.delete(id)) throw notFound(id)
    }

    private fun notFound(id: Int) = ResourceNotFoundException("Equipment with ID $id not found")

    /** Проверяет поля и возвращает копию с обрезанными пробелами. */
    private fun validated(request: EquipmentRequest): EquipmentRequest {
        val name = request.name.trim()
        val labName = request.labName.trim()

        if (name.isEmpty() || name.length > 100) {
            throw ValidationException("name must be between 1 and 100 characters")
        }
        if (labName.isEmpty() || labName.length > 50) {
            throw ValidationException("labName must be between 1 and 50 characters")
        }
        if (request.hourlyRate < 0) {
            throw ValidationException("hourlyRate must not be negative")
        }
        return request.copy(name = name, labName = labName)
    }
}
