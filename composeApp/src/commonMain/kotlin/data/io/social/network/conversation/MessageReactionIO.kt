package data.io.social.network.conversation

import kotlinx.serialization.Serializable

/** A singular reaction to a message */
@Serializable
data class MessageReactionIO(
    /** message content */
    val content: String? = null,

    /** Public id of the author of this message */
    val authorPublicId: String? = null
)