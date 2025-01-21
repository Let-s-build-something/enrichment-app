package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MatrixEvent(
    /** Required: The fields in this object will vary depending on the type of event. */
    val content: MatrixEventContent? = null,
    /** Required: The type of event. This SHOULD be namespaced similar to Java package naming conventions e.g. ‘com.example.subdomain.event.type’ */
    val type: String? = null,
) {

    @Serializable
    data class StrippedStateEvent(
        /** Required: The state_key for the event. */
        @SerialName("state_key")
        val stateKey: String? = null,

        /** Required: Contains the fully-qualified ID of the user who sent this event. */
        val sender: String? = null,
    )

    @Serializable
    data class RoomClientEvent(
        /** Required: The globally unique event identifier. */
        @SerialName("event_id")
        val eventId: String? = null,

        /** Required: The time the event was sent to the server as a unix timestamp in milliseconds. */
        @SerialName("origin_server_ts")
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