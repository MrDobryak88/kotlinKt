import kotlinx.serialization.Serializable

@Serializable
data class LabEquipment(
    val name: String,
    val labName: String,
    val hourlyRate: Int,
    val isAvailable: Boolean
)