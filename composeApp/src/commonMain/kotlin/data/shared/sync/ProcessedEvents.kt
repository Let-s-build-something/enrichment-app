package data.shared.sync

import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO

data class ProcessedEvents(
    val messages: List<ConversationMessageIO>,
    val members: List<ConversationRoomMember>
)