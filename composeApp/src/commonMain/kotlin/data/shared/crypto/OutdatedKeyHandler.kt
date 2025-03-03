package data.shared.crypto

import data.io.base.BaseResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.EncryptedRoomInfo
import data.shared.crypto.model.KeySignatureTrustLevel
import data.shared.crypto.model.StoredCrossSigningKeys
import data.shared.crypto.model.StoredDeviceKeys
import database.dao.ConversationRoomDao
import database.dao.matrix.RoomMemberDao
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.clientserverapi.model.keys.GetKeys
import net.folivo.trixnity.clientserverapi.model.sync.Sync
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.keys.CrossSigningKeys
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage
import net.folivo.trixnity.core.model.keys.DeviceKeys
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.core.model.keys.Signed
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys
import net.folivo.trixnity.crypto.olm.membershipsAllowedToReceiveKey
import net.folivo.trixnity.crypto.sign.SignService
import net.folivo.trixnity.crypto.sign.VerifyResult
import net.folivo.trixnity.crypto.sign.verify
import org.koin.mp.KoinPlatform
import ui.login.safeRequest

class OutdatedKeyHandler(
    private val homeserver: () -> String,
    private val keyStore: OlmCryptoStore,
    private val userInfo: UserInfo,
    private val signService: SignService,
    private val keyTrustService: KeyTrustService
) {
    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val roomMemberDao: RoomMemberDao by KoinPlatform.getKoin().inject()

    internal suspend fun updateOutdatedKeys() = withContext(Dispatchers.IO) {
        val userIds = keyStore.getOutdatedKeys()
        println("kostka_test, updateOutdatedKeys, userIds: $userIds")
        if (userIds.isEmpty()) return@withContext

        val keysResponse = getKeys(
            deviceKeys = userIds.associateWith { emptySet() },
        ).getOrNull()
        if (keysResponse == null) return@withContext

        println("kostka_test, updateOutdatedKeys, response: $keysResponse")
        val joinedEncryptedRooms = conversationRoomDao.getEncrypted()

        userIds.chunked(25).forEach { userIdChunk ->
            userIdChunk.forEach { userId ->
                keysResponse.masterKeys?.get(userId)?.let { masterKey ->
                    handleOutdatedCrossSigningKey(
                        userId = userId,
                        crossSigningKey = masterKey,
                        usage = CrossSigningKeysUsage.MasterKey,
                        signingKeyForVerification = masterKey.getSelfSigningKey(),
                        signingOptional = true
                    )
                }
                keysResponse.selfSigningKeys?.get(userId)?.let { selfSigningKey ->
                    handleOutdatedCrossSigningKey(
                        userId = userId,
                        crossSigningKey = selfSigningKey,
                        usage = CrossSigningKeysUsage.SelfSigningKey,
                        signingKeyForVerification = keyStore.getCrossSigningKey(
                            userId,
                            CrossSigningKeysUsage.MasterKey
                        )?.value?.signed?.get()
                    )
                }
                keysResponse.userSigningKeys?.get(userId)?.let { userSigningKey ->
                    handleOutdatedCrossSigningKey(
                        userId = userId,
                        crossSigningKey = userSigningKey,
                        usage = CrossSigningKeysUsage.UserSigningKey,
                        signingKeyForVerification = keyStore.getCrossSigningKey(
                            userId,
                            CrossSigningKeysUsage.MasterKey
                        )?.value?.signed?.get()
                    )
                }
                keysResponse.deviceKeys?.get(userId)?.let { devices ->
                    handleOutdatedDeviceKeys(
                        userId = userId,
                        devices = devices,
                        joinedEncryptedRooms = joinedEncryptedRooms
                    )
                }
                // indicate, that we fetched the keys of the user
                keyStore.updateCrossSigningKeys(userId) { it ?: setOf() }
                keyStore.updateDeviceKeys(userId) { it ?: mapOf() }

                keyStore.updateOutdatedKeys { it - userId }
            }
            //yield()
        }
    }

    internal suspend fun handleDeviceLists(deviceList: Sync.Response.DeviceLists?) {
        withContext(Dispatchers.Default) {
            println("handle new device list: $deviceList")
            val trackOwnKey = deviceList?.changed?.contains(userInfo.userId) == true
            val startTrackingKeys = deviceList?.changed?.filter { keyStore.isTracked(it) }?.toSet().orEmpty()
                .let { if (trackOwnKey) it + userInfo.userId else it }
            val stopTrackingKeys = deviceList?.left.orEmpty() - userInfo.userId
            updateKeyTracking(
                startTracking = startTrackingKeys,
                stopTracking = stopTrackingKeys,
                reason = "device list",
            )
        }
    }

    internal suspend fun updateDeviceKeysFromChangedMembership(
        room: ConversationRoomIO?,
        events: List<ClientEvent.RoomEvent.StateEvent<MemberEventContent>>
    ) = withContext(Dispatchers.IO) {
        val stopTrackingKeys = mutableSetOf<UserId>()
        val startTrackingKeys = mutableSetOf<UserId>()
        println("kostka_test, updateDeviceKeysFromChangedMembership, events: ${events.size}")
        events.forEach { event ->
            (room ?: conversationRoomDao.get(event.roomId.full))?.let { room ->
                println("kostka_test, room algorithm: ${room.algorithm}")
                if (room.algorithm != null) {
                    val userId = UserId(event.stateKey)
                    if (userId != userInfo.userId) {
                        if(keyStore.isTracked(userId)) {
                            val isActiveMemberOfAnyOtherEncryptedRoom =
                                roomMemberDao.getUserByRoomId(
                                    roomIds = conversationRoomDao.getEncrypted().map { it.id },
                                    userId = userId.full
                                ).any { event ->
                                    val membership = event.content.membership
                                    membership == Membership.JOIN || membership == Membership.INVITE
                                }
                            if (!isActiveMemberOfAnyOtherEncryptedRoom) {
                                stopTrackingKeys.add(userId)
                            }
                        }else {
                            startTrackingKeys.add(userId)
                        }
                    }
                }
            }
        }
        println("kostka_test, stopTrackingKeys: $stopTrackingKeys, startTrackingKeys: $startTrackingKeys")
        updateKeyTracking(
            startTracking = startTrackingKeys,
            stopTracking = stopTrackingKeys,
            reason = "member event",
        )
    }

    private suspend fun updateKeyTracking(startTracking: Set<UserId>, stopTracking: Set<UserId>, reason: String) {
        if (startTracking.isNotEmpty() || stopTracking.isNotEmpty()) {
            keyStore.updateOutdatedKeys { it + startTracking - stopTracking }
            stopTracking.forEach { userId ->
                keyStore.deleteDeviceKeys(userId)
                keyStore.deleteCrossSigningKeys(userId)
            }
        }
    }

    private suspend fun getKeys(
        deviceKeys: Map<UserId, Set<String>>,
        timeout: Long? = 10_000
    ): Result<GetKeys.Response> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<GetKeys.Response> {
                post(urlString = "https://${homeserver()}/_matrix/client/v3/keys/query") {
                    setBody(
                        GetKeys.Request(
                            keysFrom = deviceKeys,
                            timeout = timeout
                        )
                    )
                }
            }.let {
                if(it is BaseResponse.Success) {
                    Result.success(it.data)
                } else Result.failure(Throwable(it.toString()))
            }
        }
    }

    private suspend fun handleOutdatedCrossSigningKey(
        userId: UserId,
        crossSigningKey: Signed<CrossSigningKeys, UserId>,
        usage: CrossSigningKeysUsage,
        signingKeyForVerification: Key.Ed25519Key?,
        signingOptional: Boolean = false
    ) {
        val signatureVerification =
            signService.verify(crossSigningKey, mapOf(userId to setOfNotNull(signingKeyForVerification)))
        if (signatureVerification == VerifyResult.Valid
            || signingOptional && signatureVerification is VerifyResult.MissingSignature
        ) {
            val oldTrustLevel = keyStore.getCrossSigningKey(userId, usage)?.trustLevel
            val trustLevel = keyTrustService.calculateCrossSigningKeysTrustLevel(crossSigningKey)
            println("updated outdated cross signing ${usage.name} key of user $userId with trust level $trustLevel (was $oldTrustLevel)")
            val newKey = StoredCrossSigningKeys(crossSigningKey, trustLevel)
            keyStore.updateCrossSigningKeys(userId) { oldKeys ->
                ((oldKeys?.filterNot { it.value.signed.usage.contains(usage) }
                    ?.toSet() ?: setOf())
                        + newKey)
            }
            if (oldTrustLevel != trustLevel) {
                newKey.value.signed.get<Key.Ed25519Key>()
                    ?.let { keyTrustService.updateTrustLevelOfKeyChainSignedBy(userId, it) }
            }
        } else {
            println("Signatures from cross signing key (${usage.name}) of $userId were not valid: $signatureVerification!")
        }
    }

    private suspend fun handleOutdatedDeviceKeys(
        userId: UserId,
        devices: Map<String, SignedDeviceKeys>,
        joinedEncryptedRooms: List<EncryptedRoomInfo>
    ) {
        val oldDevices = keyStore.getDeviceKeys(userId.full)
        val newDevices = devices.filter { (deviceId, deviceKeys) ->
            val signatureVerification =
                signService.verify(deviceKeys, mapOf(userId to setOfNotNull(deviceKeys.getSelfSigningKey())))
            (userId == deviceKeys.signed.userId && deviceId == deviceKeys.signed.deviceId
                    && signatureVerification == VerifyResult.Valid)
                .also {
                    if (!it) println("Signatures from device key $deviceId of $userId were not valid: $signatureVerification!")
                }
        }.mapValues { (_, deviceKeys) ->
            val trustLevel = keyTrustService.calculateDeviceKeysTrustLevel(deviceKeys)
            println("updated outdated device keys ${deviceKeys.signed.deviceId} of user $userId with trust level $trustLevel")
            StoredDeviceKeys(deviceKeys, trustLevel)
        }
        val addedDevices = newDevices.keys - oldDevices.keys
        val removedDevices = oldDevices.keys - newDevices.keys
        // we can do this, because an outbound megolm session does only exist, when loadMembers has been called
        when {
            removedDevices.isNotEmpty() -> {
                joinedEncryptedRooms.forEach {
                    keyStore.updateOutboundMegolmSession(RoomId(it.id)) { null }
                }
            }

            addedDevices.isNotEmpty() -> {
                if (joinedEncryptedRooms.isNotEmpty()) {
                    withContext(Dispatchers.Default) {
                        val memberships = roomMemberDao.getUserByRoomId(
                            roomIds = joinedEncryptedRooms.map { it.id },
                            userId = userId.full
                        ).associate { it.roomId to it.content.membership }

                        memberships
                            .filter { (roomId, membership) ->
                                joinedEncryptedRooms.find { it.id == roomId }
                                    ?.historyVisibility
                                    ?.membershipsAllowedToReceiveKey
                                    ?.contains(membership) == true
                            }
                            .keys
                            .also {
                                if (it.isNotEmpty()) println("notify megolm sessions in rooms $it about new devices $addedDevices from $userId")
                            }.forEach { roomId ->
                                keyStore.updateOutboundMegolmSession(RoomId(roomId)) { oms ->
                                    oms?.copy(
                                        newDevices = oms.newDevices + Pair(
                                            userId,
                                            oms.newDevices[userId]?.plus(addedDevices) ?: addedDevices
                                        )
                                    )
                                }
                            }
                    }
                }
            }
        }
        keyStore.updateCrossSigningKeys(userId) { oldKeys ->
            val usersMasterKey = oldKeys?.find { it.value.signed.usage.contains(CrossSigningKeysUsage.MasterKey) }
            if (usersMasterKey != null) {
                val notFullyCrossSigned =
                    newDevices.any { it.value.trustLevel == KeySignatureTrustLevel.NotCrossSigned }
                val oldMasterKeyTrustLevel = usersMasterKey.trustLevel
                val newMasterKeyTrustLevel = when (oldMasterKeyTrustLevel) {
                    is KeySignatureTrustLevel.CrossSigned -> {
                        if (notFullyCrossSigned) {
                            println("mark master key of $userId as ${KeySignatureTrustLevel.NotAllDeviceKeysCrossSigned::class.simpleName}")
                            KeySignatureTrustLevel.NotAllDeviceKeysCrossSigned(oldMasterKeyTrustLevel.verified)
                        } else oldMasterKeyTrustLevel
                    }

                    else -> oldMasterKeyTrustLevel
                }
                if (oldMasterKeyTrustLevel != newMasterKeyTrustLevel) {
                    (oldKeys - usersMasterKey) + usersMasterKey.copy(trustLevel = newMasterKeyTrustLevel)
                } else oldKeys
            } else oldKeys
        }
        keyStore.saveDeviceKeys(userId, newDevices)
    }

    /**
     * Only DeviceKeys and CrossSigningKeys are supported.
     */
    private inline fun <reified T> Signed<T, UserId>.getSelfSigningKey(): Key.Ed25519Key? {
        return when (val signed = this.signed) {
            is DeviceKeys -> signed.keys.get()
            is CrossSigningKeys -> signed.keys.get()
            else -> null
        }
    }

    private suspend fun OlmCryptoStore.isTracked(userId: UserId): Boolean = getDeviceKeys(userId.full).isNotEmpty()
}

internal inline fun <reified T : Key> Keys.get(): T? {
    return keys.filterIsInstance<T>().firstOrNull()
}

internal inline fun <reified T : Key> CrossSigningKeys.get(): T? {
    return keys.keys.filterIsInstance<T>().firstOrNull()
}
