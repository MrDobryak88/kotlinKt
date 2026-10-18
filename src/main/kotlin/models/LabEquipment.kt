package com.example.models

import kotlinx.serialization.Serializable

/** Оборудование лаборатории (так оно возвращается клиенту). */
@Serializable
data class LabEquipment(
    val id: Int,
    val name: String,
    val labName: String,
    val hourlyRate: Int,
    val isAvailable: Boolean
)

/** Тело запроса для создания и обновления оборудования. */
@Serializable
data class EquipmentRequest(
    val name: String,
    val labName: String,
    val hourlyRate: Int,
    val isAvailable: Boolean = true
)
