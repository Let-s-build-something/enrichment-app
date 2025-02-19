package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import base.utils.Matrix
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.SyncResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.MatrixEvent
import data.io.matrix.room.MatrixEventContent
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.cryptoModule
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.RoomEventDao
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
import org.koin.core.context.loadKoinModules
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
        private const val PING_EXPIRY_MS = 60_000 * 15
    }

    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()
    private val roomEventDao: RoomEventDao by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var homeserver: String? = null
    private var nextBatch: String? = null
    private var isRunning = false

    /** Begins the synchronization process and runs it over and over as long as the app is running or stopped via [stop] */
    fun sync(homeserver: String, delay: Long? = null) {
        this.homeserver = homeserver
        if(!isRunning) {
            isRunning = true
            syncScope.launch {
                delay?.let { delay(it) }
                sharedRepository.authenticateUser(
                    localSettings = sharedDataManager.localSettings.value
                )?.let {
                    loadKoinModules(cryptoModule(sharedDataManager))
                    sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(it)
                    enqueue()
                }
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
        val owner = sharedDataManager.currentUser.value?.matrixUserId ?: return
        if(homeserver == null) return
        val batch = nextBatch ?: matrixPagingMetaDao.getByEntityId(
            entityId = "${homeserver}_$owner"
        )?.nextBatch

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
                            batch = batch,
                            owner = owner
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
        batch: String?,
        owner: String
    ) {
        matrixPagingMetaDao.insert(
            MatrixPagingMetaIO(
                entityId = "${homeserver}_$owner",
                entityType = PagingEntityType.Sync,
                nextBatch = nextBatch,
                batch = batch
            )
        )
        withContext(Dispatchers.Default) {
            val matrixRooms = response.rooms?.let { matrixRooms ->
                mutableListOf<ConversationRoomIO>().apply {
                    addAll(matrixRooms.join?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(matrixRooms.invite?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(matrixRooms.knock?.map { it.value.copy(id = it.key) }.orEmpty())
                    addAll(matrixRooms.leave?.map { it.value.copy(id = it.key) }.orEmpty())
                }
            }


            // messages
            val messages = mutableListOf<ConversationMessageIO>()
            val rooms = mutableListOf<ConversationRoomIO>()
            val receipts = mutableListOf<MatrixEvent>()
            matrixRooms?.forEach { room ->
                var alias: String? = null
                var name: String? = null
                var avatar: MatrixEventContent? = null

                mutableListOf<MatrixEvent>()
                    .apply {
                        addAll(room.accountData?.events.orEmpty())
                        addAll(room.ephemeral?.events.orEmpty())
                        addAll(room.state?.events.orEmpty())
                        addAll(room.timeline?.events.orEmpty())

                        roomEventDao.insertAll(this)
                    }.forEach { event ->
                        when(event.type) {
                            Matrix.Room.RECEIPT -> receipts.add(event)
                            Matrix.Room.CANONICAL_ALIAS -> {
                                alias = event.content?.alias ?: event.content?.altAliases?.firstOrNull()
                            }
                            Matrix.Room.NAME -> {
                                name = event.content?.name ?: name
                            }
                            Matrix.Room.AVATAR -> {
                                avatar = event.content
                            }
                            Matrix.Room.MESSAGE -> {
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
                                    state = if(receipts.find { it.eventId == event.eventId } != null) {
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
                            else -> {}
                        }
                    }

                val newItem = room.copy(
                    summary = room.summary?.copy(
                        avatar = avatar?.url?.let {
                            MediaIO(
                                url = it,
                                mimetype = avatar?.info?.mimetype,
                                name = avatar?.filename,
                                size = avatar?.info?.size
                            )
                        },
                        canonicalAlias = alias ?: name,
                        lastMessage = room.timeline?.events?.find {
                            it.type == Matrix.Room.MESSAGE
                        },
                    ),
                    ownerPublicId = owner,
                    primaryKey = "${room.id}_${owner}"
                )

                // either update existing one, or insert new one
                rooms.add(
                    conversationRoomDao.getItem(
                        id = room.id,
                        ownerPublicId = owner
                    )?.update(newItem) ?: newItem
                )
            }
            withContext(Dispatchers.IO) {
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

                if(messages.isNotEmpty()) {
                    appendPing(
                        AppPing(
                            type = AppPingType.Conversation,
                            identifiers = messages.mapNotNull { it.conversationId }.distinct()
                        )
                    )
                }
            }
            
            withContext(Dispatchers.IO) {
                if(rooms.isNotEmpty()) {
                    conversationRoomDao.insertAll(rooms)
                    appendPing(AppPing(type = AppPingType.ConversationDashboard))
                }
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
                lastPingTime = lastPingTime.coerceAtLeast(time) + 200L // buffer

                if(calculatedDelay > 0) delay(calculatedDelay)
                sharedDataManager.pingStream.value = LinkedHashSet(sharedDataManager.pingStream.value).apply {
                    retainAll {
                        DateUtils.now.toEpochMilliseconds().minus(it.timestamp) < PING_EXPIRY_MS
                    }
                }.plus(ping)
            }
        }
    }
}
