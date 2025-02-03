package data.io.social.network.conversation.matrix

import kotlinx.serialization.Serializable

/** The timeline of messages and state changes in the room. */
@Serializable
data class RoomTimeline(
    /** Required: List of events. */
    val events: List<MatrixEvent.RoomClientEvent>? = null,

    /** True if the number of events returned was limited by the limit on the filter. */
    val limited: Boolean? = null,

    val prevBatch: String? = null
)