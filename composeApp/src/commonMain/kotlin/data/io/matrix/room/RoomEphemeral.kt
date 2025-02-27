package data.io.matrix.room

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.ClientEvent.EphemeralEvent

/**
 * The new ephemeral events in the room (events that aren’t recorded in the timeline or state of the room)
 * v13: Typing notification and read receipt events
 */
@Serializable
data class RoomEphemeral(
    val events: List<@Contextual EphemeralEvent<*>>
)