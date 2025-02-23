package data.io.matrix.room.event.content

import data.io.matrix.room.event.content.RoomMessageEventContent.FileBased
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val MatrixClientEvent<*>.stateKeyOrNull: String?
    get() = when (this) {
        is MatrixClientEvent.StateBaseEvent -> this.stateKey
        else -> null
    }

val MatrixClientEvent<*>.idOrNull: EventId?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.id
        else -> null
    }

val MatrixClientEvent<*>.originTimestampOrNull: Long?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.originTimestamp
        else -> null
    }

val MatrixClientEvent<*>.roomIdOrNull: RoomId?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.roomId
        is MatrixClientEvent.StrippedStateEvent -> this.roomId
        is MatrixClientEvent.RoomAccountDataEvent -> this.roomId
        is MatrixClientEvent.EphemeralEvent -> this.roomId
        else -> null
    }

val MatrixClientEvent<*>.senderOrNull: UserId?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.sender
        is MatrixClientEvent.StrippedStateEvent -> this.sender
        is MatrixClientEvent.ToDeviceEvent -> this.sender
        is MatrixClientEvent.EphemeralEvent -> this.sender
        else -> null
    }


@OptIn(ExperimentalUuidApi::class)
suspend fun constructMessages(
    state: List<MatrixClientEvent<*>>,
    timeline: List<MatrixClientEvent<*>>,
    roomId: String,
    currentBatch: String?,
    prevBatch: String?,
    nextBatch: String?
): List<ConversationMessageIO> {
    return withContext(Dispatchers.Default) {
        val messages = mutableListOf<ConversationMessageIO>()
        val receipts = mutableListOf<MatrixClientEvent<ReceiptEventContent>>()

        // TODO update items already present in the DB
        state.forEach { event ->
            when(event.content) {
                is ReceiptEventContent -> {
                    (event as? MatrixClientEvent<ReceiptEventContent>)?.let {
                        receipts.add(it)
                    }
                }
            }
        }

        timeline.forEach { event ->
            when(val content = event.content) {
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
