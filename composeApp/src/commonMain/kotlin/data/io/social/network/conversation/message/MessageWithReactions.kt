package data.io.social.network.conversation.message

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import kotlin.jvm.Transient

data class MessageWithReactions(
    @Embedded val message: ConversationMessageIO,

    @Relation(
        parentColumn = "id",
        entityColumn = "message_id"
    )
    val reactions: List<MessageReactionIO>
) {
    @Transient
    @Ignore
    val id = message.id
}
