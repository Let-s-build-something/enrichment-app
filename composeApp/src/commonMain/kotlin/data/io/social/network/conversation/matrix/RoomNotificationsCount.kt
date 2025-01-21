package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Counts of unread notifications for a room. */
@Serializable
data class RoomNotificationsCount(
    /** The number of unread notifications for this room with the highlight flag set. */
    @SerialName("highlight_count")
    val highlightCount: Int? = null,

    /** The total number of unread notifications for this room. */
    @SerialName("notification_count")
    val notificationCount: Int? = null,
)