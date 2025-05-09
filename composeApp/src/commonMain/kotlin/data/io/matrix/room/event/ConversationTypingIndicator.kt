package data.io.matrix.room.event

import androidx.room.Ignore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.EphemeralEventContent
import ui.conversation.components.ANIMATION_LENGTH

/** Indicator of other user typing within the same conversation */
@Serializable
data class ConversationTypingIndicator(
    /** Textual content of the message being typed */
    var content: String? = null,

    @SerialName("user_ids")
    val userIds: Set<UserId>? = null,

    val type: String = "m.typing"
): EphemeralEventContent {

    val typing: Boolean = !content.isNullOrBlank()
    val timeout: Long = ANIMATION_LENGTH

    /** User attached to this message */
    @Ignore
    @Transient
    var user: ConversationRoomMember? = null

    override fun toString(): String {
        return "{" +
                "userIds: $userIds, " +
                "content: $content, " +
                "}"
    }
}
