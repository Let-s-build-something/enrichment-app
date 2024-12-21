package data.io.social.network.conversation

import data.io.base.PaginationInfo
import data.io.base.PaginationPageResponse
import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class ConversationMessagesResponse(
    override val pagination: PaginationInfo,
    override val content: List<ConversationMessageIO>
): PaginationPageResponse<ConversationMessageIO>