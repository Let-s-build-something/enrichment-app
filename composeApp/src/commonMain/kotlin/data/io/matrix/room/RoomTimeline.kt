package data.io.matrix.room

import data.io.matrix.room.event.content.MatrixClientEvent.RoomEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/** The timeline of messages and state changes in the room. */
@Serializable
data class RoomTimeline(
    /** Required: List of events. */
    val events: List<@Contextual RoomEvent<*>>? = null,

    /** True if the number of events returned was limited by the limit on the filter. */
    val limited: Boolean? = null,

    val prevBatch: String? = null
)