import org.jetbrains.exposed.sql.Table

object EquipmentTable : Table("EquipmentTable") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val labName = varchar("lab_name", 50)
    val hourlyRate = integer("hourly_rate")
    val isAvailable = bool("isAvailable")

    override val primaryKey = PrimaryKey(id)

    init {
        // В одной лаборатории может быть много приборов, но название не должно повторяться
        uniqueIndex(name, labName)
    }
}
