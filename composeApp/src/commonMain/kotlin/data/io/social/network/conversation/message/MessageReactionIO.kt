package data.io.social.network.conversation.message

import data.io.matrix.room.event.ConversationRoomMember
import kotlinx.serialization.Serializable

/** A singular reaction to a message */
@Serializable
data class MessageReactionIO(
    /** message content */
    val content: String? = null,

    /** Public id of the author of this message */
    val authorPublicId: String? = null,

    val user: ConversationRoomMember? = null
)
