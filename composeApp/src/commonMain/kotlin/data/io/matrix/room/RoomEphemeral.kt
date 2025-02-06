package data.io.matrix.room

import kotlinx.serialization.Serializable

/**
 * The new ephemeral events in the room (events that aren’t recorded in the timeline or state of the room)
 * v13: Typing notification and read receipt events
 */
@Serializable
data class RoomEphemeral(
    val events: List<MatrixEvent.RoomClientEvent>
)