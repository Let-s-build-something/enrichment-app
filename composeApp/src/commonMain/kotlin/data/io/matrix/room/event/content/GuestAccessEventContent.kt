package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @see <a href="https://spec.matrix.org/v1.10/client-server-api/#mroomguest_access">matrix spec</a>
 */
@Serializable
data class GuestAccessEventContent(
    @SerialName("guest_access")
    val guestAccess: GuestAccessType,
    @SerialName("external_url")
    override val externalUrl: String? = null,
) : StateEventContent {
    @Serializable
    enum class GuestAccessType {
        @SerialName("can_join")
        CAN_JOIN,

        @SerialName("forbidden")
        FORBIDDEN
    }
}