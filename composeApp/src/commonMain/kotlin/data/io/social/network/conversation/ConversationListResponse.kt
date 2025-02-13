package data.io.social.network.conversation

import data.io.base.paging.PaginationInfo
import data.io.base.paging.PaginationPageResponse
import data.io.matrix.room.ConversationRoomIO
import kotlinx.serialization.Serializable

@Serializable
data class ConversationListResponse(
    override val pagination: PaginationInfo? = null,
    override val content: List<ConversationRoomIO>
): PaginationPageResponse<ConversationRoomIO>