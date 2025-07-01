package data.io.social.network.conversation.message

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import data.io.matrix.room.event.ConversationRoomMember
import kotlin.jvm.Transient

data class FullConversationMessage(
    @Embedded val data: ConversationMessageIO,

    @Relation(
        parentColumn = "author_public_id",
        entityColumn = "userId",
        entity = ConversationRoomMember::class
    )
    val author: ConversationRoomMember? = null,

    @Relation(
        parentColumn = "anchor_message_id",
        entityColumn = "id",
        entity = ConversationMessageIO::class
    )
    val anchorMessage: ConversationMessageIO? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "message_id"
    )
    val reactions: List<MessageReactionIO>
) {

    @Transient
    @Ignore
    val id = data.id
}
