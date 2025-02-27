package data.io.matrix.room

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.ClientEvent.RoomAccountDataEvent

/** Typing notification and read receipt events */
@Serializable
data class RoomAccountData(
    val events: List<@Contextual RoomAccountDataEvent<*>>? = null
)