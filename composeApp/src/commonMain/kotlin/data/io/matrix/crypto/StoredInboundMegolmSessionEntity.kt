package data.io.matrix.crypto

import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.crypto.olm.StoredInboundMegolmSession

@Entity(AppRoomDatabase.TABLE_INBOUND_MEGOLM_SESSION)
@Serializable
data class StoredInboundMegolmSessionEntity(
    val senderKey: Key.Curve25519Key,
    val senderSigningKey: Key.Ed25519Key,
    val sessionId: String,
    val roomId: RoomId? = null,
    val firstKnownIndex: Long,
    val hasBeenBackedUp: Boolean,
    /**
     * This means, that we can trust the communication channel from which we received the session from.
     * For example the key backup cannot be trusted due to async encryption.
     * This does NOT mean, that we trust this megolm session. It needs to be checked whether we trust the sender key.
     */
    val isTrusted: Boolean,
    val forwardingCurve25519KeyChain: List<Key.Curve25519Key>,
    val pickled: String,

    @PrimaryKey
    val id: String = "${roomId?.full}-$sessionId"
) {
    val asStoredInboundMegolmSession: StoredInboundMegolmSession
        get() = StoredInboundMegolmSession(
            senderKey = senderKey,
            senderSigningKey = senderSigningKey,
            sessionId = sessionId,
            roomId = roomId ?: RoomId(""),
            firstKnownIndex = firstKnownIndex,
            hasBeenBackedUp = hasBeenBackedUp,
            isTrusted = isTrusted,
            forwardingCurve25519KeyChain = forwardingCurve25519KeyChain,
            pickled = pickled
        )
}

val StoredInboundMegolmSession.asStoredInboundMegolmSessionEntity: StoredInboundMegolmSessionEntity
    get() = StoredInboundMegolmSessionEntity(
        senderKey = senderKey,
        senderSigningKey = senderSigningKey,
        sessionId = sessionId,
        roomId = roomId,
        firstKnownIndex = firstKnownIndex,
        hasBeenBackedUp = hasBeenBackedUp,
        isTrusted = isTrusted,
        forwardingCurve25519KeyChain = forwardingCurve25519KeyChain,
        pickled = pickled
    )
