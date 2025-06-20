package data.io.social.network.conversation.message

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.ClientEvent.EphemeralEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent

/** Response to the request for circling requests */
@Serializable
data class ConversationMessagesResponse(
    val chunk: List<@Contextual RoomEvent<*>>? = null,
    val state: List<@Contextual EphemeralEvent<*>>? = null,
    val end: String? = null,
    val start: String? = null
)