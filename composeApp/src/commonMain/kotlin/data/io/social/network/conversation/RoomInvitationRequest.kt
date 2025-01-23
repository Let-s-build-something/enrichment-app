package data.io.social.network.conversation

import kotlinx.serialization.Serializable

/** Request for a new invitation */
@Serializable
data class RoomInvitationRequest(
    /** Identifier of the raget conversation */
    val conversationId: String?,

    /** Identifier of the user to be invited */
    val userPublicIds: List<String>?,

    /** Message attached to the invitation event */
    val message: String?
)