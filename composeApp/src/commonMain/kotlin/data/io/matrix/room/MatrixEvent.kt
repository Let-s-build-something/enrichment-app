package data.io.matrix.room

import kotlinx.serialization.Serializable

@Serializable
open class MatrixEvent(
    /** Required: The fields in this object will vary depending on the type of event. */
    val content: MatrixEventContent? = null,
    /** Required: The type of event. This SHOULD be namespaced similar to Java package naming conventions e.g. ‘com.example.subdomain.event.type’ */
    val type: String? = null,
) {
    @Serializable
    data class StrippedStateEvent(
        /** Required: The state_key for the event. */
        val stateKey: String? = null,

        /** Required: Contains the fully-qualified ID of the user who sent this event. */
        val sender: String? = null,
    ): MatrixEvent()

    @Serializable
    data class RoomClientEvent(
        /** Required: The globally unique event identifier. */
        val eventId: String? = null,

        /** Required: The time the event was sent to the server as a unix timestamp in milliseconds. */
        val originServerTs: Long? = null,

        /** Required: Contains the fully-qualified ID of the user who sent this event. */
        val sender: String? = null,

        /** Present if, and only if, this event is a state event.
         *  The key making this piece of state unique in the room. */
        val state: String? = null,

        /** Contains optional extra information about the event. */
        val unsigned: MatrixEventUnsigned? = null
    ): MatrixEvent()
}