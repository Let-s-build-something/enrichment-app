package data.io.social.network.conversation

import kotlinx.serialization.Serializable

/** Response from an invitation */
@Serializable
data class InvitationResponse(
    /** Identifier of a new conversation */
    val conversationId: String? = null,

    /** Alias of the new conversation */
    val alias: String? = null
)
