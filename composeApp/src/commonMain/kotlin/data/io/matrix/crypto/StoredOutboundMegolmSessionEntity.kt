package data.io.matrix.crypto

import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.olm.StoredOutboundMegolmSession
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(AppRoomDatabase.TABLE_OUTBOUND_MEGOLM_SESSION)
@Serializable
data class StoredOutboundMegolmSessionEntity @OptIn(ExperimentalUuidApi::class) constructor(
    val roomId: RoomId,
    val createdAt: Instant = Clock.System.now(),
    val encryptedMessageCount: Long = 1,
    val newDevices: Map<UserId, Set<String>> = mapOf(),
    val pickled: String,

    @PrimaryKey
    val id: String = roomId.full.ifBlank { Uuid.random().toString() }
) {
    val asStoredOutboundMegolmSession: StoredOutboundMegolmSession
        get() = StoredOutboundMegolmSession(
            roomId = roomId,
            createdAt = createdAt,
            encryptedMessageCount = encryptedMessageCount,
            newDevices = newDevices,
            pickled = pickled
        )
}

val StoredOutboundMegolmSession.asStoredOutboundMegolmSessionEntity: StoredOutboundMegolmSessionEntity
    get() = StoredOutboundMegolmSessionEntity(
        roomId = roomId,
        createdAt = createdAt,
        encryptedMessageCount = encryptedMessageCount,
        newDevices = newDevices,
        pickled = pickled
    )
