package data.shared.sync

import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent

data class ProcessedEvents(
    val messages: List<ConversationMessageIO>,
    val members: List<ConversationRoomMember>,
    val memberUpdates: List<RoomEvent.StateEvent<MemberEventContent>>,
    val receipts: List<ClientEvent<ReceiptEventContent>>,
    /** Message id to the encrypted event */
    val encryptedEvents: List<Pair<String, RoomEvent.MessageEvent<*>>>
)