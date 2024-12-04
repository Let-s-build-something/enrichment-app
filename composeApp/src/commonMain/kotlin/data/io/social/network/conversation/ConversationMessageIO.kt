package data.io.social.network.conversation

import kotlinx.serialization.Serializable

/** Conversation entity representing a singular message within a conversation */
@Serializable
data class ConversationMessageIO(

    /** Message identifier */
    val id: String? = null,

    /** message content */
    val content: String? = null,

    /** Public id of the author of this message */
    val authorPublicId: String? = null,

    /** Tag of the author */
    val tag: String? = null,

    /** List of reactions to this message */
    val reactions: List<MessageReactionIO>? = null,

    /** Time of creation */
    val createdAt: Long? = null,

    /**
     * State of this message. Generally, this information is sent only for the last item,
     * as it represents all of the messages above it
     */
    val state: MessageState? = null
)
