package data.io.social.network.conversation

import kotlinx.serialization.Serializable

/** Request for a new invitation */
@Serializable
data class RoomInvitationRequest(
    /** Identifier of the raget conversation */
    val conversationId: String? = null,

    /** Identifier of the user to be invited */
    val userPublicIds: List<String>? = null,

    /** Message attached to the invitation event */
    val message: String? = null,

    /** Whether a new room should be created as a result of this invitation */
    val newRoomName: String? = null
)