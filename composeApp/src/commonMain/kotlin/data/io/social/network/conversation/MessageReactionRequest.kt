package data.io.social.network.conversation

import kotlinx.serialization.Serializable

/** Body of a request for reaction to a message */
@Serializable
data class MessageReactionRequest(
    /** content of the reaction */
    val content: String,

    /** identifier of a message */
    val messageId: String,
)
