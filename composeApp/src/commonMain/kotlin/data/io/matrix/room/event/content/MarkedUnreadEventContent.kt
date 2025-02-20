package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.RoomAccountDataEventContent

@Serializable
data class MarkedUnreadEventContent(
    @SerialName("unread") val unread: Boolean,
) : RoomAccountDataEventContent