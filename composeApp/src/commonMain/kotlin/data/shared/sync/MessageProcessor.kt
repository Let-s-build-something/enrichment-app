package data.shared.sync

import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.message_decryption_failed
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import data.shared.SharedDataManager
import data.shared.sync.EventUtils.asMessage
import database.dao.ConversationMessageDao
import database.dao.matrix.RoomMemberDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.room.decrypt
import net.folivo.trixnity.client.roomEventEncryptionServices
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedToDeviceEventContent.OlmEncryptedToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.model.events.stateKeyOrNull
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

abstract class MessageProcessor {

    protected val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()
    private val roomMemberDao: RoomMemberDao by KoinPlatform.getKoin().inject()

    protected val decryptionScope = CoroutineScope(Dispatchers.Default)

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalUuidApi::class)
    suspend fun processEvents(
        events: List<ClientEvent<*>>,
        roomId: String,
        currentBatch: String?,
        prevBatch: String?,
        nextBatch: String?
    ): ProcessedEvents {
        return withContext(Dispatchers.Default) {
            val messages = mutableListOf<ConversationMessageIO>()
            val members = mutableListOf<ConversationRoomMember>()
            val memberUpdates = mutableListOf<RoomEvent.StateEvent<MemberEventContent>>()
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
                        content.displayName?.let { displayName ->
                            messages.add(
                                ConversationMessageIO(
                                    content = content.membership.asMessage(
                                        isSelf = content.displayName == sharedDataManager.currentUser.value?.matrixDisplayName,
                                        displayName = displayName
                                    ),
                                    sentAt = event.originTimestampOrNull?.let { millis ->
                                        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                                    },
                                    conversationId = roomId,
                                    state = MessageState.Sent,
                                    authorPublicId = event.senderOrNull?.full,
                                    id = event.idOrNull?.full ?: Uuid.random().toString(),
                                    currentBatch = currentBatch,
                                    prevBatch = prevBatch,
                                    nextBatch = nextBatch
                                )
                            )
                        }
                        if(event is RoomEvent.StateEvent) {
                            (event as? RoomEvent.StateEvent<MemberEventContent>)?.let {
                                memberUpdates.add(it)
                            }
                        }
                    }
                    is ReceiptEventContent -> {
                        (event as? ClientEvent<ReceiptEventContent>)?.let {
                            receipts.add(it)
                        }
                    }
                    is OlmEncryptedToDeviceEventContent, is MegolmEncryptedMessageEventContent -> {
                        if(event is RoomEvent.MessageEvent) {
                            decryptEvent(event)
                        }
                        messages.add(
                            ConversationMessageIO(
                                content = getString(Res.string.message_decryption_failed),
                                sentAt = event.originTimestampOrNull?.let { millis ->
                                    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                                },
                                conversationId = roomId,
                                state = MessageState.Sent,
                                authorPublicId = event.senderOrNull?.full,
                                id = event.idOrNull?.full ?: Uuid.random().toString(),
                                currentBatch = currentBatch,
                                prevBatch = prevBatch,
                                nextBatch = nextBatch
                            )
                        )
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
                    else -> {
                        messages.add(
                            ConversationMessageIO(
                                sentAt = event.originTimestampOrNull?.let { millis ->
                                    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                                },
                                conversationId = roomId,
                                state = MessageState.Sent,
                                authorPublicId = event.senderOrNull?.full,
                                id = event.idOrNull?.full ?: Uuid.random().toString(),
                                currentBatch = currentBatch,
                                prevBatch = prevBatch,
                                nextBatch = nextBatch
                            ))
                    }
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

            withContext(Dispatchers.IO) {
                roomMemberDao.insertAll(members)

                conversationMessageDao.insertAll(messages)
                // add the anchor messages
                val updates = withContext(Dispatchers.Default) {
                    messages.filter { it.anchorMessageId != null }.map {
                        it.copy(anchorMessage = withContext(Dispatchers.IO) {
                            conversationMessageDao.get(it.anchorMessageId)?.toAnchorMessage()
                        })
                    }
                }
                conversationMessageDao.insertAll(updates)
            }

            ProcessedEvents(
                messages = messages,
                members = members
            )
        }
    }

    private fun decryptEvent(event: RoomEvent.MessageEvent<*>) {
        decryptionScope.launch {
            sharedDataManager.matrixClient.value?.roomEventEncryptionServices?.decrypt(event)
        }
    }
}