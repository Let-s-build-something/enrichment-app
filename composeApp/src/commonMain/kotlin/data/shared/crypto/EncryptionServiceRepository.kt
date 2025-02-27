package data.shared.crypto

import data.io.base.BaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.model.keys.ClaimKeys
import net.folivo.trixnity.clientserverapi.model.keys.SetKeys
import net.folivo.trixnity.clientserverapi.model.users.SendToDevice
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ToDeviceEventContent
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
    private val homeserver: () -> String
): OlmEncryptionServiceRequestHandler {
    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()
    private val contentMappings: EventContentSerializerMappings by KoinPlatform.getKoin().inject()

    suspend fun setOneTimeKeys(
        deviceKeys: SignedDeviceKeys?,
        oneTimeKeys: Keys?,
        fallbackKeys: Keys?
    ): Result<Map<KeyAlgorithm, Int>> {
        httpClient.safeRequest<SetKeys.Response> {
            put(urlString = "https://${homeserver()}/_matrix/client/v3/keys/upload") {
                setBody(
                    SetKeys.Request(
                        oneTimeKeys = oneTimeKeys,
                        fallbackKeys = fallbackKeys,
                        deviceKeys = deviceKeys
                    )
                )
            }
        }.let {
            return if(it is BaseResponse.Success) {
                Result.success(it.data.oneTimeKeyCounts)
            } else Result.failure(Throwable(it.toString()))
        }
    }

    override suspend fun claimKeys(oneTimeKeys: Map<UserId, Map<String, KeyAlgorithm>>): Result<ClaimKeys.Response> {
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
            return if(it is BaseResponse.Success) Result.success(it.data) else Result.failure(Throwable(it.toString()))
        }
    }

    override suspend fun sendToDevice(events: Map<UserId, Map<String, ToDeviceEventContent>>): Result<Unit> = runCatching {
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