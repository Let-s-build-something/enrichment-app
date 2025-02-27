package data.io.matrix.room.event.content

import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedToDeviceEventContent.OlmEncryptedToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.crypto.olm.OlmEncryptionService
import org.koin.mp.KoinPlatform
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
suspend fun constructMessages(
    state: List<ClientEvent<*>>,
    timeline: List<ClientEvent<*>>,
    roomId: String,
    currentBatch: String?,
    prevBatch: String?,
    nextBatch: String?
): List<ConversationMessageIO> {
    val encryptionService: OlmEncryptionService by KoinPlatform.getKoin().inject()

    return withContext(Dispatchers.Default) {
        val messages = mutableListOf<ConversationMessageIO>()
        val receipts = mutableListOf<ClientEvent<ReceiptEventContent>>()

        state.forEach { event ->
            when(val content = event.content) {
                is ReceiptEventContent -> {
                    (event as? ClientEvent<ReceiptEventContent>)?.let {
                        receipts.add(it)
                    }
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

        timeline.forEach { event ->
            when(val content = event.content) {
                is OlmEncryptedToDeviceEventContent -> {
                    (event as? ClientEvent.ToDeviceEvent<OlmEncryptedToDeviceEventContent>)?.let { encrypted ->
                        encryptionService.decryptOlm(event = encrypted).also {
                            println("kostka_test, decrypted olm event: $it")
                        }
                    }
                }
                is MegolmEncryptedMessageEventContent -> {
                    (event as? ClientEvent.RoomEvent<MegolmEncryptedMessageEventContent>)?.let { encrypted ->
                        encryptionService.decryptMegolm(encryptedEvent = encrypted).also {
                            println("kostka_test, decrypted megolm event: $it")
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
            }
        }

        messages
    }
}
