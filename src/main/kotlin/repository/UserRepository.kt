import com.example.models.User

interface UserRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun remove(email: String)
    suspend fun create(email: String, passwordHash: String, role: String): User
    suspend fun getAll(): List<User>
    suspend fun getHash(email: String): String?
    suspend fun findById(id: Int): User?
    suspend fun remove(id: Int)

}