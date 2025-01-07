package data.io.social.network.conversation

import androidx.room.Ignore
import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Indicator of other user typing within the same conversation */
@Serializable
data class ConversationTypingIndicator(
    /** Public identifier of the author */
    val authorPublicId: String? = null,

    /** Textual content of the message being typed */
    var content: String? = null
) {

    /** User attached to this message */
    @Ignore
    @Transient
    var user: NetworkItemIO? = null

    override fun toString(): String {
        return "{" +
                "authorPublicId: $authorPublicId, " +
                "content: $content, " +
                "}"
    }
}
