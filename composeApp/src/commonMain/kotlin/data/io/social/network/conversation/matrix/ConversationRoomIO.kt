package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationRoomIO(
    /** Unique identifier of this room, in the format of "!opaque_id:domain" */
    val id: String? = null,

    /** Information about the room */
    val summary: RoomSummary? = null,

    /** Counts of unread notifications for this room. */
    @SerialName("unread_notifications")
    val unreadNotifications: RoomNotificationsCount? = null,

    /** Typing notification and read receipt events */
    val ephemeral: RoomEphemeral? = null,

    /** The private data that this user has attached to this room. */
    @SerialName("account_data")
    val accountData: RoomAccountData? = null,

    /** The timeline of messages and state changes in the room. */
    val timeline: RoomTimeline? = null,

    /** Type of the room */
    val type: RoomType? = null
)
