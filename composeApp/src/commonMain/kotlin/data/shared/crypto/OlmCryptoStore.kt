package data.shared.crypto

import base.utils.Matrix
import data.io.InstantAsStringSerializer
import data.io.app.SecureSettingsKeys.KEY_DEVICE_KEY
import data.io.app.SecureSettingsKeys.KEY_FALLBACK_INSTANT
import data.io.app.SecureSettingsKeys.KEY_OLM_ACCOUNT
import data.shared.SharedDataManager
import database.dao.ConversationRoomDao
import database.dao.RoomEventDao
import koin.SecureAppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.keys.DeviceKeys
import net.folivo.trixnity.core.model.keys.EncryptionAlgorithm
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys
import net.folivo.trixnity.crypto.olm.OlmStore
import net.folivo.trixnity.crypto.olm.StoredInboundMegolmMessageIndex
import net.folivo.trixnity.crypto.olm.StoredInboundMegolmSession
import net.folivo.trixnity.crypto.olm.StoredOlmSession
import net.folivo.trixnity.crypto.olm.StoredOutboundMegolmSession
import org.koin.mp.KoinPlatform

class OlmCryptoStore(
    private val sharedDataManager: SharedDataManager
): OlmStore {
    private val secureSettings: SecureAppSettings by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val roomEventDao: RoomEventDao by KoinPlatform.getKoin().inject()

    override suspend fun getInboundMegolmSession(
        sessionId: String,
        roomId: RoomId
    ): StoredInboundMegolmSession? {
        TODO("Not yet implemented")
    }

    override suspend fun updateInboundMegolmMessageIndex(
        sessionId: String,
        roomId: RoomId,
        messageIndex: Long,
        updater: suspend (StoredInboundMegolmMessageIndex?) -> StoredInboundMegolmMessageIndex?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateInboundMegolmSession(
        sessionId: String,
        roomId: RoomId,
        updater: suspend (StoredInboundMegolmSession?) -> StoredInboundMegolmSession?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateOlmSessions(
        senderKey: Key.Curve25519Key,
        updater: suspend (Set<StoredOlmSession>?) -> Set<StoredOlmSession>?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateOutboundMegolmSession(
        roomId: RoomId,
        updater: suspend (StoredOutboundMegolmSession?) -> StoredOutboundMegolmSession?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getForgetFallbackKeyAfter(): Flow<Instant?> {
        return flow {
            secureSettings.getStringOrNull(
                key = composeKey(KEY_FALLBACK_INSTANT),
            )?.takeIf { it.isNotBlank() }?.let { value ->
                json.decodeFromString(value)
            }
        }
    }

    override suspend fun updateForgetFallbackKeyAfter(updater: suspend (Instant?) -> Instant?) {
        secureSettings.putString(
            key = composeKey(KEY_FALLBACK_INSTANT),
            value = updater.invoke(getForgetFallbackKeyAfter().firstOrNull())?.let { safeInstant ->
                json.encodeToString(
                    value = safeInstant,
                    serializer = InstantAsStringSerializer
                )
            } ?: ""
        )
    }

    override suspend fun getHistoryVisibility(roomId: RoomId): HistoryVisibilityEventContent.HistoryVisibility? {
        return roomEventDao.filterStateItems(
            roomId = roomId.full,
            stateKey = "",
            type = Matrix.Room.HISTORY_VISIBILITY
        )?.firstOrNull()?.content?.historyVisibility
    }

    override suspend fun getRoomEncryptionAlgorithm(roomId: RoomId): EncryptionAlgorithm? {
        return roomEventDao.filterStateItems(
            roomId = roomId.full,
            stateKey = "",
            type = Matrix.Room.ENCRYPTION
        )?.firstOrNull()?.content?.algorithm
    }

    override suspend fun findCurve25519Key(userId: UserId, deviceId: String): Key.Curve25519Key? =
        getDeviceKey(userId.full, deviceId)?.value?.get<Key.Curve25519Key>()

    override suspend fun findEd25519Key(userId: UserId, deviceId: String): Key.Ed25519Key? =
        getDeviceKey(userId.full, deviceId)?.value?.get<Key.Ed25519Key>()

    override suspend fun findDeviceKeys(userId: UserId, senderKey: Key.Curve25519Key): DeviceKeys? =
        getDeviceKeys(userId.full).values.map { it.value.signed }
            .find { it.keys.keys.any { key -> key.value == senderKey.value } }

    override suspend fun getDevices(roomId: RoomId, userId: UserId): Set<String> {
        return getDeviceKeys(userId.full).keys
    }

    override suspend fun getDevices(
        roomId: RoomId,
        memberships: Set<Membership>
    ): Map<UserId, Set<String>>? {
        return conversationRoomDao.getItem(
            id = roomId.full,
            ownerPublicId = sharedDataManager.currentUser.value?.matrixUserId
        )?.summary?.heroes?.associate { userId ->
            getDeviceKeys(userId).let { UserId(userId) to it.keys }
        }
    }

    override suspend fun getOlmAccount(): String {
        return secureSettings.getString(
            key = composeKey(KEY_OLM_ACCOUNT),
            defaultValue = ""
        )
    }

    override suspend fun updateOlmAccount(updater: suspend (String) -> String) {
        secureSettings.putString(
            key = composeKey(KEY_OLM_ACCOUNT),
            value = updater.invoke(getOlmAccount())
        )
    }

    override suspend fun getOlmPickleKey(): String = sharedDataManager.localSettings.value?.pickleKey ?: ""

    private fun composeKey(key: String): String = "${key}_${sharedDataManager.currentUser.value?.matrixUserId}"

    private fun getDeviceKey(userId: String, deviceId: String) = getDeviceKeys(userId)[deviceId]
    private fun getDeviceKeys(userId: String): Map<String, StoredDeviceKeys> {
        return secureSettings.getStringOrNull(
            key = composeKey("${KEY_DEVICE_KEY}_$userId")
        )?.let {
            json.decodeFromString<Map<String, StoredDeviceKeys>>(it)
        } ?: emptyMap()
    }
    internal inline fun <reified T : Key> SignedDeviceKeys.get(): T? {
        return signed.keys.keys.filterIsInstance<T>().firstOrNull()
    }
}
