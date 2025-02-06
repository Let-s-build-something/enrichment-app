package data.io.matrix.room

import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable

/**
 * Information about the room which clients may need to correctly render it to users.
 *
 * https://spec.matrix.org/v1.13/client-server-api/#get_matrixclientv3sync_response-200_roomsummary
 */
@Serializable
data class RoomSummary(
    /** This should be the first 5 members of the room, ordered by stream ordering, which are joined or invited.
     *  The list must never include the client’s own user ID. */
    val heroes: List<String>? = null,

    /** List of members */
    val members: List<NetworkItemIO>? = null,

    /** Name of the room. */
    val canonicalAlias: String? = null,

    /** Tag - non-unique identification of the room. */
    val tag: String? = null,

    /** Avatar photo url. */
    val avatarUrl: String? = null,

    /** The room’s canonical alias. */
    val proximity: Float? = null,

    /** Last message that happened in this room. */
    val lastMessage: MatrixEventContent? = null,

    /** Whether this room is just one on one. */
    val isDirect: Boolean? = null,

    /** Message sent out to invited people by default if not changed. */
    val invitationMessage: String? = null,

    /** The number of users with membership of invite. */
    val invitedMembersCount: Int? = null,

    /** The number of users with membership of join, including the client’s own user ID. */
    val joinedMemberCount: Int? = null
) {
    /** Either [canonicalAlias] or a default based on [heroes] */
    val alias: String
        get() = canonicalAlias ?: heroes?.joinToString(", ") ?: "Room $tag"
}
