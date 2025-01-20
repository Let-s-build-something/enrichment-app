package data.io.social.network.conversation.matrix

import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matrix conversation room object */
@Entity(tableName = AppRoomDatabase.ROOM_CONVERSATION_ROOM_TABLE)
@Serializable
data class ConversationRoomIO(
    /** Unique identifier of this room, in the format of "!opaque_id:domain" */
    @PrimaryKey
    val id: String? = null,

    /** Information about the room */
    val summary: RoomSummary? = null,

    /** Counts of unread notifications for this room. */
    @SerialName("unread_notifications")
    val unreadNotifications: RoomNotificationsCount? = null,

    /** Typing notification and read receipt events */
    val ephemeral: RoomEphemeral? = null,

    /** The state updates for the room up to the start of the timeline. */
    val state: RoomEphemeral? = null,

    /** The private data that this user has attached to this room. */
    @SerialName("account_data")
    val accountData: RoomAccountData? = null,

    /** The timeline of messages and state changes in the room. */
    val timeline: RoomTimeline? = null,

    /** The stripped state of a room that the user has been invited to. */
    @SerialName("invite_state")
    val inviteState: RoomInviteState? = null,

    /** The stripped state of a room that the user has knocked upon. */
    @SerialName("knock_state")
    val knockState: RoomInviteState? = null,

    /**
     * A decimal range between -1 and 10. -1 means blocked, 1 is muted,
     *  or just a far social circle, and 10 is the closest
     */
    val proximity: Float? = null
) {

    /** To which batch this data object belongs to */
    var batch: String? = null

    /** Next batch identification if any */
    var nextBatch: String? = null

    /** Type of the room */
    val type: RoomType
        get() = when {
            inviteState != null -> RoomType.Invited
            knockState != null -> RoomType.Knocked
            state != null -> RoomType.Left
            else -> RoomType.Joined
        }
}
