package data.io.social.network.conversation.message

import data.io.base.paging.PaginationInfo
import data.io.base.paging.PaginationPageResponse
import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class ConversationMessagesResponse(
    override val pagination: PaginationInfo,
    override val content: List<ConversationMessageIO>
): PaginationPageResponse<ConversationMessageIO>