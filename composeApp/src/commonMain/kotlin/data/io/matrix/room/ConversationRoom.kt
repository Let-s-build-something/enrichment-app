package data.io.matrix.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import data.io.user.NetworkItemIO
import database.AppRoomDatabase
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Matrix conversation room object */
@Entity(tableName = AppRoomDatabase.TABLE_CONVERSATION_ROOM)
@Serializable
data class ConversationRoomIO @OptIn(ExperimentalUuidApi::class) constructor(
    /** Unique identifier of this room, in the format of "!opaque_id:domain" */
    val id: String = Uuid.random().toString(),

    /** Information about the room */
    val summary: RoomSummary? = null,

    /**
     * A decimal range between -1 and 10. -1 means blocked, 1 is muted,
     *  or just a far social circle, and 10 is the closest
     */
    val proximity: Float? = null,

    /** Counts of unread notifications for this room. */
    @ColumnInfo("unread_notifications")
    val unreadNotifications: RoomNotificationsCount? = null,

    /** Typing notification and read receipt events */
    val ephemeral: RoomEphemeral? = null,

    /** The state updates for the room up to the start of the timeline. */
    val state: RoomEphemeral? = null,

    /** The private data that this user has attached to this room. */
    @ColumnInfo("account_data")
    val accountData: RoomAccountData? = null,

    /** The stripped state of a room that the user has been invited to. */
    @ColumnInfo("invite_state")
    val inviteState: RoomInviteState? = null,

    /** The stripped state of a room that the user has knocked upon. */
    @ColumnInfo("knock_state")
    val knockState: RoomInviteState? = null,

    /** Database flag: an identifier of the owner of this item */
    @ColumnInfo("owner_public_id")
    val ownerPublicId: String? = null,

    @PrimaryKey
    @ColumnInfo("primary_key")
    val primaryKey: String = "${id}_$ownerPublicId"
) {

    /** The timeline of messages and state changes in the room. */
    @Ignore
    var timeline: RoomTimeline? = null

    fun update(other: ConversationRoomIO?): ConversationRoomIO {
        return if(other == null) this
        else this.copy(
            id = other.id,
            summary = summary?.update(other.summary) ?: other.summary,
            proximity = other.proximity ?: proximity,
            unreadNotifications = other.unreadNotifications ?: unreadNotifications,
            ephemeral = other.ephemeral ?: ephemeral,
            state = other.state ?: state,
            accountData = other.accountData ?: accountData,
            inviteState = other.inviteState ?: inviteState,
            knockState = other.knockState ?: knockState,
            ownerPublicId = other.ownerPublicId ?: ownerPublicId,
            primaryKey = other.primaryKey
        ).apply {
            this.timeline = other.timeline ?: this.timeline
        }
    }

    /** Type of the room */
    val type: RoomType
        get() = when {
            inviteState != null -> RoomType.Invited
            knockState != null -> RoomType.Knocked
            state != null -> RoomType.Left
            else -> RoomType.Joined
        }

    /** Converts this item to a network item representation */
    fun toNetworkItem() = NetworkItemIO(
        publicId = id,
        name = summary?.alias,
        tag = summary?.tag,
        photoUrl = summary?.avatarUrl,
        lastMessage = summary?.lastMessage?.content?.body
    )
}
