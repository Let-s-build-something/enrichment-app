package data.shared.crypto

import data.io.app.SecureSettingsKeys.KEY_CROSS_SIGNING_KEY
import data.io.app.SecureSettingsKeys.KEY_DEVICE_KEY
import data.io.app.SecureSettingsKeys.KEY_FALLBACK_INSTANT
import data.io.app.SecureSettingsKeys.KEY_OLM_ACCOUNT
import data.io.app.SecureSettingsKeys.KEY_SECRETS
import data.io.app.SecureSettingsKeys.KEY_SECRET_KEY_EVENT
import data.io.matrix.crypto.OutdatedKey
import data.io.matrix.crypto.asStoredInboundMegolmMessageIndexEntity
import data.io.matrix.crypto.asStoredInboundMegolmSessionEntity
import data.io.matrix.crypto.asStoredOlmSessionEntity
import data.io.matrix.crypto.asStoredOutboundMegolmSessionEntity
import data.shared.SharedDataManager
import data.shared.crypto.model.KeyChainLink
import data.shared.crypto.model.KeyVerificationState
import data.shared.crypto.model.StoredCrossSigningKeys
import data.shared.crypto.model.StoredDeviceKeys
import data.shared.crypto.model.StoredSecret
import database.dao.ConversationRoomDao
import database.dao.matrix.InboundMegolmSessionDao
import database.dao.matrix.KeyChainLinkDao
import database.dao.matrix.MegolmMessageIndexDao
import database.dao.matrix.OlmSessionDao
import database.dao.matrix.OutboundMegolmSessionDao
import database.dao.matrix.OutdatedKeyDao
import koin.SecureAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage
import net.folivo.trixnity.core.model.keys.DeviceKeys
import net.folivo.trixnity.core.model.keys.EncryptionAlgorithm
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys
import net.folivo.trixnity.crypto.SecretType
import net.folivo.trixnity.crypto.olm.OlmStore
import net.folivo.trixnity.crypto.olm.StoredInboundMegolmMessageIndex
import net.folivo.trixnity.crypto.olm.StoredInboundMegolmSession
import net.folivo.trixnity.crypto.olm.StoredOlmSession
import net.folivo.trixnity.crypto.olm.StoredOutboundMegolmSession
import org.koin.mp.KoinPlatform

class OlmCryptoStore(
    private val sharedDataManager: SharedDataManager
): OlmStore {
    val secureSettings: SecureAppSettings by KoinPlatform.getKoin().inject()
    val json: Json by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val olmSessionDao: OlmSessionDao by KoinPlatform.getKoin().inject()
    private val outboundMegolmSessionDao: OutboundMegolmSessionDao by KoinPlatform.getKoin().inject()
    private val inboundMegolmSessionDao: InboundMegolmSessionDao by KoinPlatform.getKoin().inject()
    private val megolmMessageIndexDao: MegolmMessageIndexDao by KoinPlatform.getKoin().inject()
    private val outdatedKeyDao: OutdatedKeyDao by KoinPlatform.getKoin().inject()
    private val keyChainLinkDao: KeyChainLinkDao by KoinPlatform.getKoin().inject()

    private val ownerId: String?
        get() = sharedDataManager.currentUser.value?.matrixUserId

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            secureSettings.remove(composeKey(KEY_OLM_ACCOUNT))
            secureSettings.remove(composeKey(KEY_FALLBACK_INSTANT))
        }
    }

    override suspend fun updateInboundMegolmMessageIndex(
        sessionId: String,
        roomId: RoomId,
        messageIndex: Long,
        updater: suspend (StoredInboundMegolmMessageIndex?) -> StoredInboundMegolmMessageIndex?
    ) {
        withContext(Dispatchers.IO) {
            val res = megolmMessageIndexDao.get(
                sessionId = sessionId,
                roomId = roomId,
                messageIndex = messageIndex
            )
            withContext(Dispatchers.Default) {
                updater.invoke(res?.asStoredInboundMegolmMessageIndex)?.also { updated ->
                    megolmMessageIndexDao.insert(updated.asStoredInboundMegolmMessageIndexEntity)
                }
            }
        }
    }

    override suspend fun updateInboundMegolmSession(
        sessionId: String,
        roomId: RoomId,
        updater: suspend (StoredInboundMegolmSession?) -> StoredInboundMegolmSession?
    ) {
        println("kostka_test, updateInboundMegolmSession, sessionId: $sessionId, roomId: $roomId")
        withContext(Dispatchers.IO) {
            val res = inboundMegolmSessionDao.get(
                sessionId = sessionId,
                roomId = roomId
            )
            withContext(Dispatchers.Default) {
                updater.invoke(res?.asStoredInboundMegolmSession)?.also { updated ->
                    inboundMegolmSessionDao.insert(updated.asStoredInboundMegolmSessionEntity)
                }
            }
        }
    }

    override suspend fun getInboundMegolmSession(
        sessionId: String,
        roomId: RoomId
    ): StoredInboundMegolmSession? {
        println("kostka_test, getInboundMegolmSession, sessionId: $sessionId, roomId: $roomId")
        return withContext(Dispatchers.IO) {
            inboundMegolmSessionDao.get(
                sessionId = sessionId,
                roomId = roomId
            )?.asStoredInboundMegolmSession
        }
    }

    suspend fun getOutdatedKeys(): Set<UserId> = withContext(Dispatchers.IO) {
        outdatedKeyDao.getAll().map { it.userId }.toSet()
    }

    suspend fun updateOutdatedKeys(updater: suspend (Set<UserId>) -> Set<UserId>) {
        withContext(Dispatchers.IO) {
            val res = getOutdatedKeys()
            withContext(Dispatchers.Default) {
                updater.invoke(res).also { updated ->
                    withContext(Dispatchers.IO) {
                        outdatedKeyDao.insertAll(updated.map { OutdatedKey(it) })
                    }
                }
            }
        }
    }

    override suspend fun updateOutboundMegolmSession(
        roomId: RoomId,
        updater: suspend (StoredOutboundMegolmSession?) -> StoredOutboundMegolmSession?
    ) {
        withContext(Dispatchers.IO) {
            val res = outboundMegolmSessionDao.get(roomId = roomId.full)
            withContext(Dispatchers.Default) {
                updater.invoke(res?.asStoredOutboundMegolmSession).also { updated ->
                    if(updated != null) {
                        outboundMegolmSessionDao.insert(updated.asStoredOutboundMegolmSessionEntity)
                    }else outboundMegolmSessionDao.remove(roomId.full)
                }
            }
        }
    }

    override suspend fun updateOlmSessions(
        senderKey: Key.Curve25519Key,
        updater: suspend (Set<StoredOlmSession>?) -> Set<StoredOlmSession>?
    ) {
        withContext(Dispatchers.IO) {
            val res = olmSessionDao.getSentItems(senderKey = senderKey.fullKeyId)
            withContext(Dispatchers.Default) {
                updater.invoke(res.map { it.asStoredOlmSession }.toSet())?.also { updated ->
                    olmSessionDao.insertAll(updated.map { it.asStoredOlmSessionEntity })
                }
            }
        }
    }

    override suspend fun getForgetFallbackKeyAfter(): Flow<Instant?> {
        return flow {
            emit(
                secureSettings.getStringOrNull(
                    key = composeKey(KEY_FALLBACK_INSTANT),
                )?.takeIf { it.isNotBlank() }?.let { value ->
                    json.decodeFromString(value)
                }
            )
        }
    }

    override suspend fun updateForgetFallbackKeyAfter(updater: suspend (Instant?) -> Instant?) {
        secureSettings.putString(
            key = composeKey(KEY_FALLBACK_INSTANT),
            value = updater.invoke(getForgetFallbackKeyAfter().firstOrNull())?.let { safeInstant ->
                json.encodeToString(
                    value = safeInstant,
                    serializer = InstantIso8601Serializer
                )
            } ?: ""
        )
    }

    override suspend fun getHistoryVisibility(roomId: RoomId): HistoryVisibilityEventContent.HistoryVisibility? {
        return conversationRoomDao.getHistoryVisibility(
            id = roomId.full,
            ownerPublicId = ownerId
        )
    }

    override suspend fun getRoomEncryptionAlgorithm(roomId: RoomId): EncryptionAlgorithm? {
        return conversationRoomDao.getAlgorithm(
            id = roomId.full,
            ownerPublicId = ownerId
        )
    }

    override suspend fun findCurve25519Key(userId: UserId, deviceId: String): Key.Curve25519Key? =
        getDeviceKey(userId.full, deviceId)?.value?.get<Key.Curve25519Key>()

    override suspend fun findEd25519Key(userId: UserId, deviceId: String): Key.Ed25519Key? =
        getDeviceKey(userId.full, deviceId)?.value?.get<Key.Ed25519Key>()

    suspend fun getSecrets(): Map<SecretType, StoredSecret>  = withContext(Dispatchers.IO) {
        secureSettings.getStringOrNull(
            key = composeKey(KEY_SECRETS)
        )?.let {
            json.decodeFromString<Map<SecretType, StoredSecret>>(it)
        } ?: emptyMap()
    }

    suspend fun updateSecrets(
        updater: suspend (Map<SecretType, StoredSecret>) -> Map<SecretType, StoredSecret>
    ) = withContext(Dispatchers.IO) {
        secureSettings.putString(
            key = composeKey(KEY_SECRETS),
            value = json.encodeToString(updater.invoke(getSecrets()))
        )
    }

    override suspend fun findDeviceKeys(userId: UserId, senderKey: Key.Curve25519Key): DeviceKeys? {
        return withContext(Dispatchers.Default) {
            getDeviceKeys(userId.full).values
                .map { it.value.signed }
                .find { it.keys.keys.any { key -> key.value == senderKey.value } }
        }
    }

    internal suspend fun getDeviceKeys(userId: String): Map<String, StoredDeviceKeys> {
        return withContext(Dispatchers.IO) {
            secureSettings.getStringOrNull(
                key = composeKey("${KEY_DEVICE_KEY}_$userId")
            )?.let {
                json.decodeFromString<Map<String, StoredDeviceKeys>>(it)
            } ?: emptyMap()
        }
    }

    override suspend fun getDevices(roomId: RoomId, userId: UserId): Set<String> {
        return getDeviceKeys(userId.full).keys
    }

    suspend fun updateDeviceKeys(
        userId: UserId,
        updater: suspend (Map<String, StoredDeviceKeys>?) -> Map<String, StoredDeviceKeys>?
    ) {
        saveDeviceKeys(
            userId = userId,
            deviceKeys = updater.invoke(getDeviceKeys(userId.full))
        )
    }

    suspend fun saveDeviceKeys(
        userId: UserId,
        deviceKeys: Map<String, StoredDeviceKeys>?
    ) {
        if(deviceKeys == null) {
            deleteDeviceKeys(userId)
        } else {
            withContext(Dispatchers.IO) {
                secureSettings.putString(
                    key = composeKey("${KEY_DEVICE_KEY}_$userId"),
                    value = json.encodeToString(deviceKeys)
                )
            }
        }
    }

    suspend fun deleteDeviceKeys(userId: UserId) = withContext(Dispatchers.IO) {
        secureSettings.remove(composeKey("${KEY_DEVICE_KEY}_$userId"))
    }

    suspend fun getCrossSigningKeys(
        userId: UserId,
    ): Set<StoredCrossSigningKeys>? {
        return withContext(Dispatchers.Default) {
            secureSettings.getStringOrNull(
                key = composeKey("${KEY_CROSS_SIGNING_KEY}_$userId")
            )?.let {
                json.decodeFromString<Set<StoredCrossSigningKeys>>(it)
            }
        }
    }

    suspend fun updateCrossSigningKeys(
        userId: UserId,
        updater: suspend (Set<StoredCrossSigningKeys>?) -> Set<StoredCrossSigningKeys>?
    ) {
        withContext(Dispatchers.IO) {
            updater.invoke(getCrossSigningKeys(userId)).also { updated ->
                if (updated == null) deleteDeviceKeys(userId)
                else secureSettings.putString(
                    key = composeKey("${KEY_CROSS_SIGNING_KEY}_$userId"),
                    value = json.encodeToString(updated)
                )
            }
        }
    }

    suspend fun deleteCrossSigningKeys(userId: UserId) = withContext(Dispatchers.IO) {
        secureSettings.remove(composeKey("${KEY_CROSS_SIGNING_KEY}_$userId"))
    }

    suspend fun getKeyVerificationState(
        key: Key,
    ): KeyVerificationState? {
        return withContext(Dispatchers.IO) {
            key.keyId?.let { keyId ->
                secureSettings.getStringOrNull(
                    key = "${keyId}_${key.algorithm.name}"
                )?.let {
                    json.decodeFromString<KeyVerificationState>(it)
                }?.let { state ->
                    if (state.keyValue == key.value) state
                    else KeyVerificationState.Blocked(state.keyValue)
                }
            }
        }
    }

    suspend fun saveKeyVerificationState(
        key: Key,
        state: KeyVerificationState
    ) {
        withContext(Dispatchers.IO) {
            key.keyId?.let { keyId ->
                secureSettings.putString(
                    key = "$${keyId}_${key.algorithm.name}",
                    value = withContext(Dispatchers.Default) { json.encodeToString(state) }
                )
            }
        }
    }

    suspend fun saveKeyChainLink(keyChainLink: KeyChainLink) {
        withContext(Dispatchers.IO) {
            keyChainLinkDao.insert(keyChainLink)
        }
    }

    suspend inline fun <reified T: SecretKeyEventContent>getSecretKeyEvent(key: String = "") = withContext(Dispatchers.IO) {
        secureSettings.getStringOrNull(
            key = composeKey("${KEY_SECRET_KEY_EVENT}_${key.takeIf { it.isNotBlank() } ?: T::class.toString()}")
        )?.let {
            json.decodeFromString<SecretKeyEventContent>(it)
        }
    }

    suspend inline fun <reified T: SecretKeyEventContent>saveSecretKeyEvent(content: ClientEvent.GlobalAccountDataEvent<T>) {
        withContext(Dispatchers.IO) {
            secureSettings.putString(
                key = composeKey("${KEY_SECRET_KEY_EVENT}_${content.key.takeIf { it.isNotBlank() } ?: T::class.toString()}"),
                value = json.encodeToString(content)
            )
        }
    }

    suspend fun getKeyChainLinksBySigningKey(userId: UserId, signingKey: Key.Ed25519Key): Set<KeyChainLink> {
        return withContext(Dispatchers.Default) {
            keyChainLinkDao.getByUserId(userId = userId.full)
                .filter {
                    it.signingKey.keyId == signingKey.keyId
                            && it.signingKey.value == signingKey.value
                }
                .toSet()
        }
    }

    suspend fun deleteKeyChainLinksBySignedKey(userId: UserId, signedKey: Key.Ed25519Key) {
        withContext(Dispatchers.IO) {
            keyChainLinkDao.getByUserId(userId = userId.full)
                .filter {
                    it.signedKey.keyId == signedKey.keyId
                            && it.signedKey.value == signedKey.value
                }.also { chains ->
                    keyChainLinkDao.removeWhere(chains.map { it.id })
                }
        }
    }

    override suspend fun getDevices(
        roomId: RoomId,
        memberships: Set<Membership>
    ): Map<UserId, Set<String>>? {
        return conversationRoomDao.getItem(
            id = roomId.full,
            ownerPublicId = ownerId
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

    fun composeKey(key: String): String = "${key}_$ownerId"

    internal suspend inline fun getCrossSigningKey(
        userId: UserId,
        usage: CrossSigningKeysUsage,
    ): StoredCrossSigningKeys? {
        return withContext(Dispatchers.Default) {
            getCrossSigningKeys(userId)?.firstOrNull {
                it.value.signed.usage.contains(usage)
            }
        }
    }

    internal suspend inline fun getCrossSigningKey(
        userId: UserId,
        keyId: String,
    ): StoredCrossSigningKeys? {
        return withContext(Dispatchers.Default) {
            getCrossSigningKeys(userId)?.firstOrNull {
                it.value.signed.keys.any { it.keyId == keyId }
            }
        }
    }

    suspend fun getDeviceKey(userId: String, deviceId: String) = getDeviceKeys(userId)[deviceId]
    internal inline fun <reified T : Key> SignedDeviceKeys.get(): T? {
        return signed.keys.keys.filterIsInstance<T>().firstOrNull()
    }
}
