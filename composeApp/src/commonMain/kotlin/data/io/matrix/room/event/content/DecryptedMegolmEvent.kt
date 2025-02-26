package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId

@Serializable
data class DecryptedMegolmEvent<C : MessageEventContent>(
    @SerialName("content") override val content: C,
    @SerialName("room_id") val roomId: RoomId? = null
) : Event<C>