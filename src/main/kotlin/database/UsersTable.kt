import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email",255).uniqueIndex()
    val passwordHash = varchar("password",100)
    val role = varchar("role",50)

    override val primaryKey = PrimaryKey(id)
}