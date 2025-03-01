package data.shared.crypto

import data.io.base.BaseResponse
import data.shared.crypto.model.isVerified
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import net.folivo.trixnity.clientserverapi.model.keys.AddSignatures
import net.folivo.trixnity.clientserverapi.model.keys.ClaimKeys
import net.folivo.trixnity.clientserverapi.model.keys.SetKeys
import net.folivo.trixnity.clientserverapi.model.users.SendToDevice
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.KeyRequestAction
import net.folivo.trixnity.core.model.events.m.RoomKeyRequestEventContent
import net.folivo.trixnity.core.model.keys.EncryptionAlgorithm
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.contentType
import net.folivo.trixnity.crypto.olm.OlmEncryptionServiceRequestHandler
import org.koin.mp.KoinPlatform
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EncryptionServiceRepository(
    private val homeserver: () -> String,
    private val keyStore: OlmCryptoStore,
    userInfo: UserInfo
): OlmEncryptionServiceRequestHandler {
    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()
    private val contentMappings: EventContentSerializerMappings by KoinPlatform.getKoin().inject()

    private val ownDeviceId: String = userInfo.deviceId
    private val ownUserId: UserId = userInfo.userId

    @OptIn(ExperimentalUuidApi::class)
    suspend fun requestRoomKeys(
        roomId: RoomId,
        sessionId: String,
    ) {
        withContext(Dispatchers.IO) {
            val receiverDeviceIds = keyStore.getDeviceKeys(ownUserId.full)
                .filter { it.value.trustLevel.isVerified }
                .map { it.value.value.signed.deviceId }
                .minus(ownDeviceId)
                .toSet()

            println("kostka_test, making a request for room keys, receiverDeviceIds: $receiverDeviceIds (${keyStore.getDeviceKeys(ownUserId.full)})")
            if (receiverDeviceIds.isEmpty()) return@withContext

            val request = RoomKeyRequestEventContent(
                action = KeyRequestAction.REQUEST,
                requestingDeviceId = ownDeviceId,
                requestId = Uuid.random().toString(),
                body = RoomKeyRequestEventContent.RequestedKeyInfo(
                    roomId = roomId,
                    sessionId = sessionId,
                    algorithm = EncryptionAlgorithm.Megolm,
                )
            )

            sendToDevice(mapOf(ownUserId to receiverDeviceIds.associateWith { request })).getOrThrow()
        }
    }

    suspend fun setOneTimeKeys(
        deviceKeys: SignedDeviceKeys?,
        oneTimeKeys: Keys?,
        fallbackKeys: Keys?
    ): Result<Map<KeyAlgorithm, Int>> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<SetKeys.Response> {
                post(urlString = "https://${homeserver()}/_matrix/client/v3/keys/upload") {
                    setBody(
                        SetKeys.Request(
                            oneTimeKeys = oneTimeKeys,
                            fallbackKeys = fallbackKeys,
                            deviceKeys = deviceKeys
                        )
                    )
                }
            }.let {
                if(it is BaseResponse.Success) {
                    Result.success(it.data.oneTimeKeyCounts)
                } else Result.failure(Throwable(it.toString()))
            }
        }
    }

    override suspend fun claimKeys(oneTimeKeys: Map<UserId, Map<String, KeyAlgorithm>>): Result<ClaimKeys.Response> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ClaimKeys.Response> {
                put(urlString = "https://${homeserver()}/_matrix/client/v3/keys/claim") {
                    setBody(
                        ClaimKeys.Request(
                            oneTimeKeys = oneTimeKeys,
                            timeout = 10_000
                        )
                    )
                }
            }.let {
                if(it is BaseResponse.Success) Result.success(it.data) else Result.failure(Throwable(it.toString()))
            }
        }
    }

    suspend fun addSignatures(
        signedDeviceKeys: Set<SignedDeviceKeys>,
        signedCrossSigningKeys: Set<SignedCrossSigningKeys>
    ): AddSignatures.Response? {
        return httpClient.safeRequest<AddSignatures.Response> {
            put(urlString = "https://${homeserver()}/_matrix/client/v3/keys/signatures/upload") {
                setBody(
                    (signedDeviceKeys.associate {
                        Pair(it.signed.userId, it.signed.deviceId) to json.encodeToJsonElement(it)
                    } + signedCrossSigningKeys.associate {
                        Pair(
                            it.signed.userId, it.signed.keys.keys.filterIsInstance<Key.Ed25519Key>().first().value
                        ) to json.encodeToJsonElement(it)
                    }).entries.groupBy { it.key.first }
                        .map { group -> group.key to group.value.associate { it.key.second to it.value } }.toMap()
                )
            }
        }.success?.data
    }

    override suspend fun sendToDevice(events: Map<UserId, Map<String, ToDeviceEventContent>>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            data class FlatEntry(
                val userId: UserId,
                val deviceId: String,
                val event: ToDeviceEventContent
            )

            val flatEvents = events.flatMap { (userId, deviceEvents) ->
                deviceEvents.map { (deviceId, deviceEvent) ->
                    FlatEntry(userId, deviceId, deviceEvent)
                }
            }
            if (flatEvents.isNotEmpty()) {
                val eventsByType = flatEvents
                    .groupBy { it.event::class }
                    .mapValues { (_, flatEntryByUserId) ->
                        flatEntryByUserId.groupBy { it.userId }
                            .mapValues { (_, flatEntryByDeviceId) ->
                                flatEntryByDeviceId.associate { it.deviceId to it.event }
                            }
                    }
                coroutineScope {
                    eventsByType.values.forEach {
                        launch {
                            sendToDeviceCheck(it).getOrThrow()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun sendToDeviceCheck(
        events: Map<UserId, Map<String, ToDeviceEventContent>>,
        transactionId: String = Uuid.random().toString()
    ): Result<Unit> {
        val firstEventForType = events.entries.firstOrNull()?.value?.entries?.firstOrNull()?.value
        requireNotNull(firstEventForType) { "you need to send at least on event" }
        require(events.flatMap { it.value.values }
            .all { it.instanceOf(firstEventForType::class) }) { "all events must be of the same type" }
        val type = contentMappings.toDevice.contentType(firstEventForType)

        return sendToDevice(
            eventType = type,
            events = events,
            transactionId = transactionId
        )
    }

    private suspend fun sendToDevice(
        eventType: String,
        events: Map<UserId, Map<String, ToDeviceEventContent>>,
        transactionId: String
    ): Result<Unit> {
        val request = SendToDevice.Request(messages = events)

        httpClient.safeRequest<Unit> {
            put(urlString = "https://${homeserver()}/_matrix/client/v3/sendToDevice/${eventType}/${transactionId}") {
                setBody(
                    json.encodeToJsonElement(
                        serializer = SendToDevice(
                            type = eventType,
                            txnId = transactionId,
                            asUserId = null
                        ).requestSerializerBuilder(
                            json = json,
                            value = request,
                            mappings = contentMappings
                        ),
                        value = request
                    )
                )
            }
        }.let {
            return if(it is BaseResponse.Success) Result.success(it.data) else Result.failure(Throwable(it.toString()))
        }
    }
}