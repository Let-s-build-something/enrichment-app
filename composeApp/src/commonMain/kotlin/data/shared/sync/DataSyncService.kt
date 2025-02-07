package data.shared.sync

import base.utils.Matrix
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.SyncResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import data.shared.SharedDataManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.home.HomeRepository.Companion.INITIAL_BATCH
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
        matrixPagingMetaDao.insert(
            MatrixPagingMetaIO(
                entityId = homeserver,
                entityType = PagingEntityType.Sync,
                nextBatch = nextBatch,
                batch = batch
            )
        )
        withContext(Dispatchers.Default) {
            val rooms = response.rooms?.let {
                mutableListOf<ConversationRoomIO>().apply {
                    addAll(it.join?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(it.invite?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(it.knock?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(it.leave?.map { it.value.copy(id = it.key) }.orEmpty())
                }
            }
            rooms?.map { item ->
                MatrixPagingMetaIO(
                    entityId = item.id,
                    nextBatch = nextBatch,
                    entityType = PagingEntityType.ConversationRoom,
                    batch = batch ?: INITIAL_BATCH
                )
            }?.let { pagingItems ->
                withContext(Dispatchers.IO) {
                    matrixPagingMetaDao.insertAll(pagingItems)
                }

                // rooms
                val values = rooms.map { previous ->
                    val alias = previous.timeline?.events?.find {
                        it.type == Matrix.Room.CANONICAL_ALIAS
                    }?.content?.let { it.alias ?: it.altAliases?.firstOrNull() }
                    val name = previous.timeline?.events?.find {
                        it.type == Matrix.Room.NAME
                    }?.content?.name

                    previous.copy(
                        summary = previous.summary?.copy(
                            avatarUrl = previous.state?.events?.find {
                                it.type == Matrix.Room.AVATAR
                            }?.content?.url,
                            canonicalAlias = alias ?: name,
                            lastMessage = previous.timeline?.events?.find {
                                it.type == Matrix.Room.MESSAGE
                            },
                        ),
                        ownerPublicId = sharedDataManager.currentUser.value?.publicId,
                        primaryKey = "${previous.id}_${sharedDataManager.currentUser.value?.publicId}"
                    ).also { room ->
                        room.batch = batch ?: INITIAL_BATCH
                        room.nextBatch = nextBatch
                    }
                }
                withContext(Dispatchers.IO) {
                    conversationRoomDao.insertAll(values)
                }

                // messages
                val messages = mutableListOf<ConversationMessageIO>()
                val messagePagingMeta = mutableListOf<MatrixPagingMetaIO>()
                rooms.forEach { room ->
                    val receipts = room.ephemeral?.events?.filter {
                        it.type == Matrix.Room.RECEIPT
                    }

                    room.timeline?.events?.filter {
                        it.type == Matrix.Room.MESSAGE
                    }?.map { event ->
                        messages.add(
                            ConversationMessageIO(
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
                            ).also {
                                println("kostka_test, newMessage: $it")
                            }
                        )
                        messagePagingMeta.add(
                            MatrixPagingMetaIO(
                                batch = batch,
                                nextBatch = room.timeline.prevBatch,
                                entityId = event.eventId ?: Uuid.random().toString(),
                                entityType = PagingEntityType.ConversationMessage
                            )
                        )
                    }
                }
                withContext(Dispatchers.IO) {
                    matrixPagingMetaDao.insertAll(messagePagingMeta)
                }
                withContext(Dispatchers.IO) {
                    conversationMessageDao.insertAll(messages)
                }
            }
        }
    }
}
