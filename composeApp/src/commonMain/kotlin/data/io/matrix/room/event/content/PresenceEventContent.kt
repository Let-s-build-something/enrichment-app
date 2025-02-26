package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @see <a href="https://spec.matrix.org/v1.10/client-server-api/#mpresence">matrix spec</a>
 */
@Serializable
data class PresenceEventContent(
    @SerialName("presence")
    val presence: Presence? = null,
    val avatarUrl: String? = null,
    @SerialName("displayname")
    val displayName: String? = null,
    val lastActiveAgo: Long? = null,
    @SerialName("currently_active")
    val isCurrentlyActive: Boolean? = null,
    @SerialName("status_msg")
    val statusMessage: String? = null
) : EphemeralEventContent
