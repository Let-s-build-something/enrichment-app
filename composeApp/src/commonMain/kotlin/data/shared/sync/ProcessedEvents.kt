package data.shared.sync

import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MessageReactionIO
import data.io.user.PresenceData
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent

data class ProcessedEvents(
    val messages: List<ConversationMessageIO>,
    /** List of new member information and whether they should be added or removed */
    val members: List<Pair<Boolean, ConversationRoomMember>>,
    val receipts: List<ClientEvent<ReceiptEventContent>>,
    val redactions: List<RedactionEventContent>,
    val presenceData: List<PresenceData>,
    val replacements: HashMap<String, ConversationMessageIO?>,
    val reactions: HashMap<String, MutableSet<MessageReactionIO>>,

    /** Message id to the encrypted event */
    val encryptedEvents: List<Pair<String, RoomEvent.MessageEvent<*>>>
) {
    val isEmpty: Boolean
        get() = messages.isEmpty()
                && members.isEmpty()
                && receipts.isEmpty()
                && redactions.isEmpty()
                && replacements.isEmpty()
                && reactions.isEmpty()
}