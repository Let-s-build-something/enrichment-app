package data.shared.sync

import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.message_decryption_failed
import data.io.base.AppPing
import data.io.base.AppPingType
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

    protected val dataService: DataService by KoinPlatform.getKoin().inject()
    protected val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()
    private val roomMemberDao: RoomMemberDao by KoinPlatform.getKoin().inject()

    protected val decryptionScope = CoroutineScope(Dispatchers.Default)


    data class SaveEventsResult(
        val messages: List<ConversationMessageIO>,
        val events: Int,
        val members: Int,
        val prevBatch: String?
    )

    suspend fun saveEvents(
        events: List<ClientEvent<*>>,
        roomId: String,
        prevBatch: String?
    ): SaveEventsResult {
        return withContext(Dispatchers.IO) {
            val result = processEvents(
                events = events,
                roomId = roomId,
                prevBatch = prevBatch
            )

            result.encryptedEvents.forEach { encrypted ->
                saveEncryptedEvent(
                    messageId = encrypted.first,
                    event = encrypted.second
                )
            }
            roomMemberDao.insertAll(result.members)

            val messages = result.messages.mapNotNull {
                if(conversationMessageDao.insertIgnore(it) == -1L) {
                    conversationMessageDao.insertReplace(it)
                    null
                } else it
            }

            if(result.messages.isNotEmpty()) {
                // add the anchor messages
                val updates = withContext(Dispatchers.Default) {
                    result.messages.filter { it.anchorMessageId != null }.map {
                        it.copy(anchorMessage = withContext(Dispatchers.IO) {
                            conversationMessageDao.get(it.anchorMessageId)?.toAnchorMessage()
                        })
                    }
                }
                conversationMessageDao.insertAll(updates)
            }

            SaveEventsResult(
                messages = messages,
                members = result.members.size,
                events = events.size,
                prevBatch = prevBatch
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    @Suppress("UNCHECKED_CAST")
    private suspend fun processEvents(
        events: List<ClientEvent<*>>,
        roomId: String,
        prevBatch: String?
    ): ProcessedEvents = withContext(Dispatchers.Default) {
        val messages = mutableListOf<ConversationMessageIO>()
        val members = mutableListOf<ConversationRoomMember>()
        val memberUpdates = mutableListOf<RoomEvent.StateEvent<MemberEventContent>>()
        val receipts = mutableListOf<ClientEvent<ReceiptEventContent>>()
        val encryptedEvents = mutableListOf<Pair<String, RoomEvent.MessageEvent<*>>>()

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
                    if(event is RoomEvent.StateEvent) {
                        (event as? RoomEvent.StateEvent<MemberEventContent>)?.let {
                            memberUpdates.add(it)
                        }
                    }
                    content.displayName?.let { displayName ->
                        ConversationMessageIO(
                            content = content.membership.asMessage(
                                isSelf = content.displayName == sharedDataManager.currentUser.value?.matrixDisplayName,
                                displayName = displayName
                            )
                        )
                    }
                }
                is ReceiptEventContent -> {
                    (event as? ClientEvent<ReceiptEventContent>)?.let {
                        // TODO save receipts to DB
                        receipts.add(it)
                    }
                    null
                }
                is OlmEncryptedToDeviceEventContent, is MegolmEncryptedMessageEventContent -> {
                    if(event is RoomEvent.MessageEvent) {
                        val id = event.idOrNull?.full ?: Uuid.random().toString()
                        encryptedEvents.add(id to event)
                        ConversationMessageIO(state = MessageState.Decrypting)
                    }else null
                }
                is MessageEventContent -> content.process()
                else -> ConversationMessageIO()
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
            }?.also { message ->
                // add general info
                messages.add(
                    message.copy(
                        id = event.idOrNull?.full ?: message.id,
                        authorPublicId = event.senderOrNull?.full,
                        sentAt = event.originTimestampOrNull?.let { millis ->
                            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                        },
                        state = message.state ?: if(receipts.find { it.idOrNull?.full == event.idOrNull?.full } != null) {
                            MessageState.Read
                        }else MessageState.Sent,
                        conversationId = roomId,
                        prevBatch = prevBatch
                    )
                )
            }
        }

        ProcessedEvents(
            messages = messages,
            members = members,
            memberUpdates = memberUpdates,
            receipts = receipts,
            encryptedEvents = encryptedEvents
        )
    }

    private fun saveEncryptedEvent(
        messageId: String,
        event: RoomEvent.MessageEvent<*>
    ) {
        decryptEvent(
            event = event,
            onResult = { content ->
                withContext(Dispatchers.IO) {
                    val message = conversationMessageDao.get(messageId)?.update(
                        content?.process() ?: ConversationMessageIO(content = getString(Res.string.message_decryption_failed))
                    )
                    message?.copy(
                        state = if(message.authorPublicId == sharedDataManager.currentUser.value?.matrixUserId) {
                            MessageState.Sent
                        } else MessageState.Read
                    )?.let { decryptedMessage ->
                        conversationMessageDao.insertReplace(decryptedMessage)
                        decryptedMessage.conversationId?.let { identifier ->
                            dataService.appendPing(
                                AppPing(
                                    type = AppPingType.Conversation,
                                    identifiers = listOf(identifier)
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    private fun decryptEvent(
        event: RoomEvent.MessageEvent<*>,
        onResult: suspend (MessageEventContent?) -> Unit
    ) {
        decryptionScope.launch {
            sharedDataManager.matrixClient.value?.roomEventEncryptionServices?.decrypt(event)?.let {
                onResult(it.getOrNull())
            }
        }
    }

    private fun MessageEventContent.process(): ConversationMessageIO {
        val file = (this as? FileBased)?.takeIf { it.url?.isBlank() == false }
        val body = (this as? RoomMessageEventContent)?.body?.takeIf {
            it != file?.body
        }
        return ConversationMessageIO(
            content = body,
            media = file?.let {
                listOf(
                    MediaIO(
                        url = it.url,
                        mimetype = it.info?.mimeType,
                        name = it.fileName,
                        size = it.info?.size
                    )
                )
            }
        )
    }
}