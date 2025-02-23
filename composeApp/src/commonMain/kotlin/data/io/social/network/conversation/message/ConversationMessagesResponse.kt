package data.io.social.network.conversation.message

import data.io.matrix.room.event.content.MatrixClientEvent.EphemeralEvent
import data.io.matrix.room.event.content.MatrixClientEvent.RoomEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class ConversationMessagesResponse(
    val chunk: List<@Contextual RoomEvent<*>>? = null,
    val state: List<@Contextual EphemeralEvent<*>>? = null,
    val end: String? = null,
    val start: String? = null
)