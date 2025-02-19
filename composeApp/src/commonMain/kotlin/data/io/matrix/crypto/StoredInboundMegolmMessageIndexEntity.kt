package data.io.matrix.crypto

import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.crypto.olm.StoredInboundMegolmMessageIndex

@Entity(AppRoomDatabase.TABLE_MEGOLM_MESSAGE_INDEX)
@Serializable
data class StoredInboundMegolmMessageIndexEntity(
    val sessionId: String,
    val roomId: RoomId,
    val messageIndex: Long,
    val eventId: EventId,
    val originTimestamp: Long,

    @PrimaryKey
    val id: String = "${roomId.full}-$sessionId-$messageIndex"
) {
    val asStoredInboundMegolmMessageIndex: StoredInboundMegolmMessageIndex
        get() = StoredInboundMegolmMessageIndex(
            sessionId = sessionId,
            roomId = roomId,
            messageIndex = messageIndex,
            eventId = eventId,
            originTimestamp = originTimestamp
        )
}

val StoredInboundMegolmMessageIndex.asStoredInboundMegolmMessageIndexEntity: StoredInboundMegolmMessageIndexEntity
    get() = StoredInboundMegolmMessageIndexEntity(
        sessionId = sessionId,
        roomId = roomId,
        messageIndex = messageIndex,
        eventId = eventId,
        originTimestamp = originTimestamp
    )
