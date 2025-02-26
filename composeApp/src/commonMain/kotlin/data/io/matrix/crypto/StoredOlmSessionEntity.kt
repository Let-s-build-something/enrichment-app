package data.io.matrix.crypto

import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase.Companion.TABLE_OLM_SESSION
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.crypto.olm.StoredOlmSession
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(TABLE_OLM_SESSION)
@Serializable
data class StoredOlmSessionEntity @OptIn(ExperimentalUuidApi::class) constructor(
    val senderKey: Key.Curve25519Key,
    val sessionId: String,
    val lastUsedAt: Instant,
    val createdAt: Instant,
    val pickled: String,
    val initiatedByThisDevice: Boolean? = false,

    @PrimaryKey
    val id: String = senderKey.fullKeyId ?: Uuid.random().toString()
) {
    val asStoredOlmSession: StoredOlmSession
        get() = StoredOlmSession(
            senderKey = senderKey,
            sessionId = sessionId,
            lastUsedAt = lastUsedAt,
            createdAt = createdAt,
            pickled = pickled
        )
}

val StoredOlmSession.asStoredOlmSessionEntity: StoredOlmSessionEntity
    get() = StoredOlmSessionEntity(
        senderKey = senderKey,
        sessionId = sessionId,
        lastUsedAt = lastUsedAt,
        createdAt = createdAt,
        pickled = pickled
    )
