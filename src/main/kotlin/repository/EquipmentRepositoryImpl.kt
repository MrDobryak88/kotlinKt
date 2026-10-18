package com.example.repository

import EquipmentTable
import com.example.Exception.AppException
import com.example.Exception.DuplicateResourceException
import com.example.Exception.InternalServerException
import com.example.models.EquipmentRequest
import com.example.models.LabEquipment
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class EquipmentRepositoryImpl : EquipmentRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction { block() }

    private fun toEquipment(row: ResultRow): LabEquipment = LabEquipment(
        id = row[EquipmentTable.id],
        name = row[EquipmentTable.name],
        labName = row[EquipmentTable.labName],
        hourlyRate = row[EquipmentTable.hourlyRate],
        isAvailable = row[EquipmentTable.isAvailable]
    )

    // 23505 — нарушение уникальности (одинаковые name + labName)
    private fun mapSqlError(e: ExposedSQLException, request: EquipmentRequest): AppException =
        if (e.sqlState == "23505") {
            DuplicateResourceException("Equipment '${request.name}' already exists in lab '${request.labName}'")
        } else {
            InternalServerException("Database error while saving equipment", e)
        }

    override suspend fun getAll(onlyAvailable: Boolean): List<LabEquipment> = dbQuery {
        val query = if (onlyAvailable) {
            EquipmentTable.selectAll().where { EquipmentTable.isAvailable eq true }
        } else {
            EquipmentTable.selectAll()
        }
        query.orderBy(EquipmentTable.id).map(::toEquipment)
    }

    override suspend fun findById(equipmentId: Int): LabEquipment? = dbQuery {
        EquipmentTable
            .selectAll()
            .where { EquipmentTable.id eq equipmentId }
            .map(::toEquipment)
            .singleOrNull()
    }

    override suspend fun create(request: EquipmentRequest): LabEquipment = dbQuery {
        try {
            val row = EquipmentTable.insert {
                it[EquipmentTable.name] = request.name
                it[EquipmentTable.labName] = request.labName
                it[EquipmentTable.hourlyRate] = request.hourlyRate
                it[EquipmentTable.isAvailable] = request.isAvailable
            }.resultedValues!!.first()
            toEquipment(row)
        } catch (e: ExposedSQLException) {
            throw mapSqlError(e, request)
        }
    }

    override suspend fun update(equipmentId: Int, request: EquipmentRequest): LabEquipment? = dbQuery {
        try {
            val updatedRows = EquipmentTable.update({ EquipmentTable.id eq equipmentId }) {
                it[EquipmentTable.name] = request.name
                it[EquipmentTable.labName] = request.labName
                it[EquipmentTable.hourlyRate] = request.hourlyRate
                it[EquipmentTable.isAvailable] = request.isAvailable
            }
            if (updatedRows == 0) {
                null
            } else {
                EquipmentTable
                    .selectAll()
                    .where { EquipmentTable.id eq equipmentId }
                    .map(::toEquipment)
                    .singleOrNull()
            }
        } catch (e: ExposedSQLException) {
            throw mapSqlError(e, request)
        }
    }

    override suspend fun delete(equipmentId: Int): Boolean = dbQuery {
        EquipmentTable.deleteWhere { EquipmentTable.id eq equipmentId } > 0
    }
}
