import kotlinx.serialization.Serializable

@Serializable
data class SubscribeRequest (
    val op : String,
    val args : List<Map<String, String>>
)