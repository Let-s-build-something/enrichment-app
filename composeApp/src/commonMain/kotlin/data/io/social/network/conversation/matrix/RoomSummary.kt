package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
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

    /** The number of users with membership of invite. */
    @SerialName("invited_member_count")
    val invitedMembersCount: Int? = null,

    /** The number of users with membership of join, including the client’s own user ID. */
    @SerialName("joined_member_count")
    val joinedMemberCount: Int? = null,
)