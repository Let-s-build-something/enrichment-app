package data.io.social.network.conversation.matrix

import kotlinx.serialization.Serializable

/** Counts of unread notifications for a room. */
@Serializable
data class RoomNotificationsCount(
    /** The number of unread notifications for this room with the highlight flag set. */
    val highlightCount: Int? = null,

    /** The total number of unread notifications for this room. */
    val notificationCount: Int? = null,
)