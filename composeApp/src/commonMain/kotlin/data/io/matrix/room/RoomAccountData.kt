package data.io.matrix.room

import data.io.matrix.room.event.content.MatrixClientEvent.RoomAccountDataEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/** Typing notification and read receipt events */
@Serializable
data class RoomAccountData(
    val events: List<@Contextual RoomAccountDataEvent<*>>? = null
)