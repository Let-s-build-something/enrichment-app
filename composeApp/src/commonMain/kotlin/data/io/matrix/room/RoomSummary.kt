package data.io.matrix.room

import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId

/**
 * Information about the room which clients may need to correctly render it to users.
 *
 * https://spec.matrix.org/v1.13/client-server-api/#get_matrixclientv3sync_response-200_roomsummary
 */
@Serializable
data class RoomSummary(
    /** This should be the first 5 members of the room, ordered by stream ordering, which are joined or invited.
     *  The list must never include the client’s own user ID. */
    @SerialName("m.heroes")
    val heroes: List<UserId>? = null,

    /** Name of the room. */
    val canonicalAlias: String? = null,

    /** Tag - non-unique identification of the room. */
    val tag: String? = null,

    /** Avatar photo url. */
    val avatar: MediaIO? = null,

    /** The room’s canonical alias. */
    val proximity: Float? = null,

    /** Whether this room is just one on one. */
    val isDirect: Boolean? = null,

    /** Message sent out to invited people by default if not changed. */
    val invitationMessage: String? = null,

    /** The number of users with membership of invite. */
    @SerialName("m.invited_member_count")
    val invitedMemberCount: Int? = null,

    /** The number of users with membership of join, including the client’s own user ID. */
    @SerialName("m.joined_member_count")
    val joinedMemberCount: Int? = null,

    /** Last message that happened in this room. */
    val lastMessage: ConversationMessageIO? = null,
) {

    fun update(other: RoomSummary?): RoomSummary {
        return if(other == null) this
        else this.copy(
            heroes = other.heroes ?: heroes,
            canonicalAlias = other.canonicalAlias ?: canonicalAlias,
            tag = other.tag ?: tag,
            avatar = other.avatar ?: avatar,
            proximity = other.proximity ?: proximity,
            lastMessage = other.lastMessage ?: lastMessage,
            isDirect = other.isDirect ?: isDirect,
            invitationMessage = other.invitationMessage ?: invitationMessage,
            invitedMemberCount = other.invitedMemberCount ?: invitedMemberCount,
            joinedMemberCount = other.joinedMemberCount ?: joinedMemberCount
        )
    }

    /** List of members */
    var members: List<NetworkItemIO>? = null

    /** Either [canonicalAlias] or a default based on [heroes] */
    val alias: String
        get() = canonicalAlias ?: heroes?.joinToString(", ") ?: "Room ${tag ?: ""}"
}
