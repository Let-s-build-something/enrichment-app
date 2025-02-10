package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import base.utils.Matrix
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.SyncResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import data.shared.SharedDataManager
import data.shared.SharedRepository
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.MatrixPagingMetaDao
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val dataSyncModule = module {
    factory { DataSyncService() }
    single { DataSyncService() }
}

class DataSyncService {
    companion object {
        const val SYNC_INTERVAL = 60_000L
    }

    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var homeserver: String? = null
    private var nextBatch: String? = null
    private var isRunning = false

    /** Begins the synchronization process and runs it over and over as long as the app is running or stopped via [stop] */
    fun sync(homeserver: String) {
        this.homeserver = homeserver
        if(!isRunning) {
            isRunning = true
            syncScope.launch {
                sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(
                    sharedRepository.authenticateUser(
                        localSettings = sharedDataManager.localSettings.value
                    )
                )
                enqueue()
            }
        }
    }

    fun stop() {
        isRunning = false
        syncScope.coroutineContext.cancelChildren()
    }

    private suspend fun enqueue(
        homeserver: String? = this.homeserver,
        nextBatch: String? = this.nextBatch
    ) {
        if(homeserver == null) return
        val batch = nextBatch ?: matrixPagingMetaDao.getByEntityId(homeserver)?.nextBatch

        httpClient.safeRequest<SyncResponse> {
            get(urlString = "https://$homeserver/_matrix/client/v3/sync") {
                parameter("timeout", SYNC_INTERVAL)
                if(batch != null) {
                    parameter("since", batch)
                }
            }
        }.let { syncResponse ->
            syncResponse.success?.data?.nextBatch?.let { nextBatch ->
                this@DataSyncService.nextBatch = nextBatch
                if(isRunning) {
                    syncResponse.success?.data?.let { data ->
                        processResponse(
                            response = data,
                            homeserver = homeserver,
                            batch = batch
                        )
                    }

                    enqueue()
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun processResponse(
        response: SyncResponse,
        homeserver: String,
        batch: String?
    ) {
        val owner = sharedDataManager.currentUser.value?.publicId ?: return

        matrixPagingMetaDao.insert(
            MatrixPagingMetaIO(
                entityId = homeserver,
                entityType = PagingEntityType.Sync,
                nextBatch = nextBatch,
                batch = batch
            )
        )
        withContext(Dispatchers.Default) {
            val rooms = response.rooms?.let { rooms ->
                mutableListOf<ConversationRoomIO>().apply {
                    addAll(rooms.join?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(rooms.invite?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(rooms.knock?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(rooms.leave?.map { it.value.copy(id = it.key) }.orEmpty())
                }
            }

            // rooms
            val values = rooms?.map { previous ->
                val alias = previous.timeline?.events?.find {
                    it.type == Matrix.Room.CANONICAL_ALIAS
                }?.content?.let { it.alias ?: it.altAliases?.firstOrNull() }
                val name = previous.timeline?.events?.find {
                    it.type == Matrix.Room.NAME
                }?.content?.name

                val newItem = previous.copy(
                    summary = previous.summary?.copy(
                        avatarUrl = previous.state?.events?.find {
                            it.type == Matrix.Room.AVATAR
                        }?.content?.url,
                        canonicalAlias = alias ?: name,
                        lastMessage = previous.timeline?.events?.find {
                            it.type == Matrix.Room.MESSAGE
                        },
                    ),
                    ownerPublicId = owner,
                    primaryKey = "${previous.id}_${owner}"
                )
                // either update existing one, or insert new one
                conversationRoomDao.getItem(
                    id = previous.id,
                    ownerPublicId = owner
                )?.update(newItem) ?: newItem
            }
            withContext(Dispatchers.IO) {
                if(!values.isNullOrEmpty()) {
                    conversationRoomDao.insertAll(values)
                    appendPing(AppPing(type = AppPingType.Conversation))
                }
            }

            // messages
            val messages = mutableListOf<ConversationMessageIO>()
            rooms?.forEach { room ->
                val receipts = room.ephemeral?.events?.filter {
                    it.type == Matrix.Room.RECEIPT
                }

                room.timeline?.events?.filter {
                    it.type == Matrix.Room.MESSAGE
                }?.forEach { event ->
                    val newItem = ConversationMessageIO(
                        content = event.content?.body?.takeIf {
                            event.content.messageType == Matrix.Message.TEXT
                        },
                        media = event.content?.url?.let {
                            listOf(
                                MediaIO(
                                    url = it,
                                    mimetype = event.content.info?.mimetype,
                                    name = event.content.filename,
                                    size = event.content.info?.size
                                )
                            )
                        },
                        sentAt = event.originServerTs?.let { millis ->
                            Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                        },
                        conversationId = room.id,
                        authorPublicId = event.sender,
                        id = event.eventId ?: Uuid.random().toString(),
                        anchorMessageId = event.content?.relatesTo?.let {
                            it.inReplyTo?.eventId ?: it.eventId
                        },
                        parentAnchorMessageId = event.content?.relatesTo?.eventId,
                        state = if(receipts?.find { it.eventId == event.eventId } != null) {
                            MessageState.Read
                        }else MessageState.Sent
                    )

                    // either update existing one, or insert new one
                    messages.add(
                        conversationMessageDao.get(
                            messageId = newItem.id
                        ) ?: newItem
                    )
                }
            }
            withContext(Dispatchers.IO) {
                conversationMessageDao.insertAll(messages)

                if(messages.isNotEmpty()) appendPing(
                    AppPing(
                        type = AppPingType.Conversation,
                        identifiers = messages.mapNotNull { it.conversationId }.distinct()
                    )
                )
            }
        }
    }

    private var lastPingTime: Long = 0L
    private val mutex = Mutex()

    private fun appendPing(ping: AppPing) {
        syncScope.launch {
            mutex.withLock {
                val time = DateUtils.now.toEpochMilliseconds()
                val calculatedDelay = if(lastPingTime == 0L) 0 else lastPingTime - time
                lastPingTime = lastPingTime.coerceAtLeast(time) + 200L // buffer of 400 milliseconds

                if(calculatedDelay > 0) delay(calculatedDelay)
                sharedDataManager.ping.emit(ping)
            }
        }
    }
}
