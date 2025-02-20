package data.io.matrix.room

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.ClientEvent.StrippedStateEvent

/** Typing notification and read receipt events */
@Serializable
data class RoomInviteState(
    val events: List<@Contextual StrippedStateEvent<*>>? = null
)