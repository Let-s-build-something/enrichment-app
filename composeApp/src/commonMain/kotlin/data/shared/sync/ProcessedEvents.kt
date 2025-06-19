package data.shared.sync

import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent

data class ProcessedEvents(
    val messages: List<ConversationMessageIO>,
    /** List of new member information and whether they should be added or removed */
    val members: List<Pair<Boolean, ConversationRoomMember>>,
    val receipts: List<ClientEvent<ReceiptEventContent>>,
    /** Message id to the encrypted event */
    val encryptedEvents: List<Pair<String, RoomEvent.MessageEvent<*>>>
)