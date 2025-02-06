package data.io.matrix.room

import kotlinx.serialization.Serializable

/** Updates to rooms. */
@Serializable
data class RoomsResponseIO(
    /** The rooms that the user has been invited to, mapped as room ID to room information. */
    val invite: Map<String, ConversationRoomIO>,

    /** The rooms that the user has joined, mapped as room ID to room information. */
    val join: Map<String, ConversationRoomIO>,

    /** The rooms that the user has knocked upon, mapped as room ID to room information. */
    val leave: Map<String, ConversationRoomIO>,

    /** The rooms that the user has left or been banned from, mapped as room ID to room information. */
    val knock: Map<String, ConversationRoomIO>
)