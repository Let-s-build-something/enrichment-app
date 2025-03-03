package data.io.matrix.room.event.content

import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import data.shared.crypto.EncryptionServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.ForwardedRoomKeyEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.RoomKeyEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedToDeviceEventContent.OlmEncryptedToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.model.events.stateKeyOrNull
import net.folivo.trixnity.crypto.olm.OlmEncryptionService
import net.folivo.trixnity.crypto.olm.OlmEncryptionService.DecryptMegolmError
import org.koin.mp.KoinPlatform
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ProcessedEvents(
    val messages: List<ConversationMessageIO>,
    val members: List<ConversationRoomMember>
)

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalUuidApi::class)
suspend fun processEvents(
    events: List<ClientEvent<*>>,
    roomId: String,
    currentBatch: String?,
    prevBatch: String?,
    nextBatch: String?
): ProcessedEvents {
    val encryptionService: OlmEncryptionService by KoinPlatform.getKoin().inject()
    val encryptionRepository: EncryptionServiceRepository by KoinPlatform.getKoin().inject()

    return withContext(Dispatchers.Default) {
        val messages = mutableListOf<ConversationMessageIO>()
        val members = mutableListOf<ConversationRoomMember>()
        val memberUpdates = mutableListOf<ClientEvent.RoomEvent.StateEvent<MemberEventContent>>()
        val receipts = mutableListOf<ClientEvent<ReceiptEventContent>>()

        events.forEach { event ->
            when(val content = event.content) {
                is MemberEventContent -> {
                    (event.stateKeyOrNull ?: content.thirdPartyInvite?.signed?.signed?.userId?.full)?.let { userId ->
                        members.add(
                            ConversationRoomMember(
                                content = content,
                                roomId = roomId,
                                timestamp = event.originTimestampOrNull,
                                sender = event.senderOrNull,
                                userId = userId
                            )
                        )
                    }
                    if(event is ClientEvent.RoomEvent.StateEvent) {
                        (event as? ClientEvent.RoomEvent.StateEvent<MemberEventContent>)?.let {
                            memberUpdates.add(it)
                        }
                    }
                }
                is RoomKeyEventContent -> {
                    println("kostka_test, RoomKeyEventContent: $event")
                }
                is ForwardedRoomKeyEventContent -> {
                    println("kostka_test, RoomKeyEventContent: $event")
                }
                is ReceiptEventContent -> {
                    (event as? ClientEvent<ReceiptEventContent>)?.let {
                        receipts.add(it)
                    }
                }
                is OlmEncryptedToDeviceEventContent -> {
                    (event as? ClientEvent.ToDeviceEvent<OlmEncryptedToDeviceEventContent>)?.let { encrypted ->
                        encryptionService.decryptOlm(event = encrypted).also {
                            println("kostka_test, decrypted olm event: $it, event: $event")
                        }
                    }
                }
                is MegolmEncryptedMessageEventContent -> {
                    (event as? ClientEvent.RoomEvent<MegolmEncryptedMessageEventContent>)?.let { encrypted ->
                        val attempt = encryptionService.decryptMegolm(encryptedEvent = encrypted)
                        when(attempt.exceptionOrNull()) {
                            is DecryptMegolmError.MegolmKeyNotFound -> {
                                // missing keys -> request them
                                encryptionRepository.requestRoomKeys(
                                    roomId = event.roomId,
                                    sessionId = content.sessionId
                                )
                                encryptionService.decryptMegolm(encryptedEvent = encrypted).getOrNull()
                            }
                            else -> attempt.getOrThrow()
                        }.also { decryptedEvent ->
                            println("kostka_test, decrypted megolm event: $decryptedEvent, event: $event")
                        }
                    }
                }
                is MessageEventContent -> {
                    val newItem = ConversationMessageIO(
                        content = (content as? RoomMessageEventContent)?.body,
                        media = (content as? FileBased)?.takeIf { it.url?.isBlank() == false }?.let {
                            listOf(
                                MediaIO(
                                    url = it.url,
                                    mimetype = it.info?.mimeType,
                                    name = it.fileName,
                                    size = it.info?.size
                                )
                            )
                        },
                        sentAt = event.originTimestampOrNull?.let { millis ->
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                        },
                        conversationId = roomId,
                        authorPublicId = event.senderOrNull?.full,
                        id = event.idOrNull?.full ?: Uuid.random().toString(),
                        anchorMessageId = content.relatesTo?.replyTo?.eventId?.full,
                        parentAnchorMessageId = content.relatesTo?.eventId?.full,
                        state = if(receipts.find { it.idOrNull?.full == event.idOrNull?.full } != null) {
                            MessageState.Read
                        }else MessageState.Sent,
                        currentBatch = currentBatch,
                        prevBatch = prevBatch,
                        nextBatch = nextBatch
                    )

                    messages.add(newItem)
                }
                else -> {}
                /*EmptyEventContent -> TODO()
                is EphemeralDataUnitContent -> TODO()
                is EphemeralEventContent -> TODO()
                is GlobalAccountDataEventContent -> TODO()
                is RoomAccountDataEventContent -> TODO()
                is MessageEventContent -> TODO()
                is RedactedEventContent -> TODO()
                is StateEventContent -> TODO()
                is UnknownEventContent -> TODO()
                is ToDeviceEventContent -> TODO()*/
            }
        }

        ProcessedEvents(
            messages = messages,
            members = members
        )
    }
}
