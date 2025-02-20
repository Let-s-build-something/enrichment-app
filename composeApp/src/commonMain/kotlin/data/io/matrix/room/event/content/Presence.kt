package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Presence(val value: String) {
    @SerialName("online")
    ONLINE("online"),

    @SerialName("offline")
    OFFLINE("offline"),

    @SerialName("unavailable")
    UNAVAILABLE("unavailable")
}