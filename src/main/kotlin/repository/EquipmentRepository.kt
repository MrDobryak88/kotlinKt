package com.example.repository

import com.example.models.EquipmentRequest
import com.example.models.LabEquipment

interface EquipmentRepository {
    suspend fun getAll(onlyAvailable: Boolean = false): List<LabEquipment>
    suspend fun findById(equipmentId: Int): LabEquipment?
    suspend fun create(request: EquipmentRequest): LabEquipment

    /** Возвращает обновлённую запись или null, если такого оборудования нет. */
    suspend fun update(equipmentId: Int, request: EquipmentRequest): LabEquipment?

    /** true, если запись была удалена. */
    suspend fun delete(equipmentId: Int): Boolean
}
