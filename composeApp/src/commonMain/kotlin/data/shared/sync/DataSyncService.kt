package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.SyncResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.event.content.constructMessages
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.user.PresenceData
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.crypto.cryptoModule
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
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
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.senderOrNull
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.conversation.ConversationRoomSource.Companion.INITIAL_BATCH
import ui.login.safeRequest

internal val dataSyncModule = module {
    factory { DataSyncService() }
    single { DataSyncService() }
}

class DataSyncService {
    companion object {
        const val SYNC_INTERVAL = 60_000L
        private const val PING_EXPIRY_MS = 60_000 * 15
        private const val START_ANEW = true
    }

    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()
    private val presenceEventDao: PresenceEventDao by KoinPlatform.getKoin().inject()

    private val clientEventEmitter: ClientEventEmitter by KoinPlatform.getKoin().inject()
    private val syncResponseEmitter: SyncResponseEmitter by KoinPlatform.getKoin().inject()

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
                (sharedDataManager.currentUser.value?.takeIf { it.publicId != null } ?: sharedRepository.authenticateUser(
                    localSettings = sharedDataManager.localSettings.value
                ))?.let {
                    sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(it) ?: it
                    loadKoinModules(cryptoModule(sharedDataManager))
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
        val pagingEntity = matrixPagingMetaDao.getByEntityId(
            entityId = "${homeserver}_$owner"
        )
        val batch = nextBatch ?: if(START_ANEW) null else pagingEntity?.nextBatch

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
                            currentBatch = batch,
                            // after getting a response, the currentBatch becomes prevBatch
                            prevBatch = pagingEntity?.currentBatch,
                            owner = owner
                        )
                    }

                    enqueue()
                }
            }
        }
    }

    private suspend fun processResponse(
        response: SyncResponse,
        homeserver: String,
        currentBatch: String?,
        prevBatch: String?,
        owner: String
    ) {
        matrixPagingMetaDao.insert(
            MatrixPagingMetaIO(
                entityId = "${homeserver}_$owner",
                entityType = PagingEntityType.Sync.name,
                nextBatch = nextBatch,
                currentBatch = currentBatch,
                prevBatch = prevBatch
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

            println("kostka_test, toDevice: ${response.toDevice}")
            syncResponseEmitter.emit(SyncEvents(
                syncResponse = response,
                allEvents = buildList {
                    response.toDevice?.events?.forEach { add(it) }
                    response.accountData?.events?.forEach { add(it) }
                    response.presence?.events?.forEach { add(it) }
                    matrixRooms?.forEach { room ->
                        addAll(room.accountData?.events.orEmpty())
                        addAll(room.ephemeral?.events.orEmpty())
                        addAll(room.state?.events.orEmpty())
                        addAll(room.timeline?.events.orEmpty())
                        addAll(room.inviteState?.events.orEmpty())
                        addAll(room.knockState?.events.orEmpty())
                    }
                }
            ))

            val messages = mutableListOf<ConversationMessageIO>()
            val rooms = mutableListOf<ConversationRoomIO>()
            val presenceContent = mutableListOf<PresenceData>()
            matrixRooms?.forEach { room ->
                var alias: String? = null
                var name: String? = null
                var avatar: AvatarEventContent? = null
                var historyVisibility: HistoryVisibilityEventContent.HistoryVisibility? = null
                var algorithm: EncryptionEventContent? = null

                mutableListOf<ClientEvent<*>>()
                    .apply {
                        addAll(response.toDevice?.events.orEmpty())
                        addAll(room.accountData?.events.orEmpty())
                        addAll(room.ephemeral?.events.orEmpty())
                        addAll(room.state?.events.orEmpty())
                        addAll(room.timeline?.events.orEmpty())
                    }
                    .map { event ->
                        with(event) {
                            when (this) {
                                is ClientEvent.RoomEvent.MessageEvent -> {
                                    this.copy(roomId = roomId.takeIf { it.full.isNotBlank() } ?: RoomId(room.id))
                                }
                                is ClientEvent.RoomEvent.StateEvent -> {
                                    this.copy(roomId = roomId.takeIf { it.full.isNotBlank() } ?: RoomId(room.id))
                                }
                                is ClientEvent.RoomAccountDataEvent -> {
                                    this.copy(roomId = roomId.takeIf { it.full.isNotBlank() } ?: RoomId(room.id))
                                }
                                is ClientEvent.StrippedStateEvent -> {
                                    this.copy(roomId = roomId.takeIf { !it?.full.isNullOrBlank() } ?: RoomId(room.id))
                                }
                                is ClientEvent.EphemeralEvent -> {
                                    this.copy(roomId = roomId.takeIf { !it?.full.isNullOrBlank() } ?: RoomId(room.id))
                                }
                                else -> event
                            }
                        }
                    }
                    .also { events ->
                        clientEventEmitter.emit(events = events)

                        constructMessages(
                            events = events,
                            prevBatch = room.prevBatch?.takeIf { room.timeline?.limited == true },
                            nextBatch = null,
                            currentBatch = INITIAL_BATCH,
                            roomId = room.id
                        ).also { newMessages ->
                            messages.addAll(newMessages)
                        }
                    }
                    .forEach { event ->
                        when(val content = event.content) {
                            is PresenceEventContent -> {
                                event.senderOrNull?.full?.let { userId ->
                                    presenceContent.add(PresenceData(userIdFull = userId, content = content))
                                }
                            }
                            is HistoryVisibilityEventContent -> historyVisibility = content.historyVisibility
                            is CanonicalAliasEventContent -> {
                                alias = (content.alias ?: content.aliases?.firstOrNull())?.full
                            }
                            is NameEventContent -> name = content.name
                            is AvatarEventContent -> avatar = content
                            is EncryptionEventContent -> algorithm = content
                            else -> {}
                        }
                    }

                val newItem = room.copy(
                    summary = room.summary?.copy(
                        avatar = avatar?.url?.let {
                            MediaIO(
                                url = it,
                                mimetype = avatar.info?.mimeType,
                                size = avatar.info?.size
                            )
                        },
                        canonicalAlias = alias ?: name,
                        lastMessage = messages.lastOrNull()?.takeIf { it.conversationId == room.id }
                    ),
                    ownerPublicId = owner,
                    primaryKey = "${room.id}_${owner}",
                    algorithm = algorithm?.algorithm
                )

                // either update existing one, or insert new one
                rooms.add(
                    conversationRoomDao.getItem(
                        id = room.id,
                        ownerPublicId = owner
                    )?.update(newItem) ?: newItem
                )
            }

            // Save presence locally
            withContext(Dispatchers.IO) {
                if(presenceContent.isNotEmpty()) {
                    presenceEventDao.insertAll(presenceContent)
                }
            }

            // Save messages locally
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

            // Save rooms locally
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
