package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.SyncResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.event.ConversationRoomMember
import data.io.matrix.room.event.content.processEvents
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.user.PresenceData
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.crypto.OlmCryptoStore
import data.shared.crypto.OutdatedKeyHandler
import data.shared.crypto.cryptoModule
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomMemberDao
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.folivo.trixnity.clientserverapi.model.rooms.GetMembers
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.model.events.stateKeyOrNull
import net.folivo.trixnity.crypto.olm.OlmEventHandler
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
        private const val START_ANEW = false
    }

    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()
    private val roomMemberDao: RoomMemberDao by KoinPlatform.getKoin().inject()
    private val presenceEventDao: PresenceEventDao by KoinPlatform.getKoin().inject()

    private val keyHandler: OutdatedKeyHandler by KoinPlatform.getKoin().inject()
    private val clientEventEmitter: ClientEventEmitter by KoinPlatform.getKoin().inject()
    private val syncResponseEmitter: SyncResponseEmitter by KoinPlatform.getKoin().inject()
    private val olmEventHandler: OlmEventHandler by KoinPlatform.getKoin().inject()

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
                    loadKoinModules(cryptoModule())
                    enqueue()
                    olmEventHandler.startInCoroutineScope(this)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        if(syncScope.isActive) {
            syncScope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun enqueue(
        homeserver: String? = this.homeserver,
        nextBatch: String? = this.nextBatch
    ) {
        val owner = sharedDataManager.currentUser.value?.matrixUserId ?: return
        if(homeserver == null) return
        val pagingEntity = if(START_ANEW) null else matrixPagingMetaDao.getByEntityId(
            entityId = "${homeserver}_$owner"
        )
        val batch = nextBatch ?: pagingEntity?.nextBatch

        //P43wqUqCsP
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
            val messages = mutableListOf<ConversationMessageIO>()
            val rooms = mutableListOf<ConversationRoomIO>()
            val presenceContent = mutableListOf<PresenceData>()

            println("kostka_test, toDevice: ${response.toDevice}, deviceLists: ${response.deviceLists}")

            val globalEvents = buildList {
                addAll(response.toDevice?.events.orEmpty())
                addAll(response.toDevice?.events.orEmpty())
                addAll(response.accountData?.events.orEmpty())
                addAll(response.presence?.events.orEmpty())
            } .onEach { event ->
                when(val content = event.content) {
                    is PresenceEventContent -> {
                        event.senderOrNull?.full?.let { userId ->
                            presenceContent.add(
                                PresenceData(
                                    userIdFull = userId,
                                    content = content
                                )
                            )
                        }
                    }

                    is SecretKeyEventContent -> {
                        if (event is ClientEvent.GlobalAccountDataEvent) {
                            @Suppress("UNCHECKED_CAST")
                            (event as? ClientEvent.GlobalAccountDataEvent<SecretKeyEventContent>)?.let {
                                KoinPlatform.getKoin().get<OlmCryptoStore>().saveSecretKeyEvent(it)
                            }
                        }
                    }
                    else -> {}
                }
            }

            keyHandler.handleDeviceLists(response.deviceLists)
            syncResponseEmitter.emit(SyncEvents(
                syncResponse = response,
                allEvents = buildList {
                    addAll(globalEvents)
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

            matrixRooms?.forEach { room ->
                var alias: String? = null
                var name: String? = null
                var avatar: AvatarEventContent? = null
                var historyVisibility: HistoryVisibilityEventContent.HistoryVisibility? = null
                var algorithm: EncryptionEventContent? = null

                val events = mutableListOf<ClientEvent<*>>()
                    .apply {
                        addAll(room.accountData?.events.orEmpty())
                        addAll(room.ephemeral?.events.orEmpty())
                        addAll(room.state?.events.orEmpty())
                        addAll(room.timeline?.events.orEmpty())
                        addAll(room.inviteState?.events.orEmpty())
                        addAll(room.knockState?.events.orEmpty())
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
                    // preprocessing of the room and adding info for further processing
                    .onEach { event ->
                        when(val content = event.content) {
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
                                mimetype = avatar?.info?.mimeType,
                                size = avatar?.info?.size
                            )
                        },
                        canonicalAlias = alias ?: name,
                        lastMessage = messages.lastOrNull()?.takeIf { it.conversationId == room.id }
                    ),
                    ownerPublicId = owner,
                    historyVisibility = historyVisibility,
                    primaryKey = "${room.id}_${owner}",
                    algorithm = algorithm?.algorithm
                )

                // either update existing one, or insert new one
                val newRoom = (conversationRoomDao.getItem(
                    id = room.id,
                    ownerPublicId = owner
                )?.update(newItem) ?: newItem).also { newRoom ->
                    conversationRoomDao.insert(newRoom)
                    rooms.add(newRoom)
                }

                // =======================  The room is saved, it's safe to proceed  ========================= //
                // ensure we have all the members
                if(events.isNotEmpty()) {
                    clientEventEmitter.emit(events = events)
                    getMembers(roomId = room.id, prevBatch = prevBatch)?.let { members ->
                        roomMemberDao.insertAll(
                            members.mapNotNull { event ->
                                (event.stateKeyOrNull ?: event.content.thirdPartyInvite?.signed?.signed?.userId?.full)?.let { userId ->
                                    ConversationRoomMember(
                                        content = event.content,
                                        roomId = room.id,
                                        timestamp = event.originTimestampOrNull,
                                        sender = event.senderOrNull,
                                        userId = userId
                                    )
                                }
                            }
                        )
                        if(newRoom.algorithm != null) {
                            keyHandler.updateDeviceKeysFromChangedMembership(
                                room = newRoom,
                                events = members
                            )
                        }
                    }
                    if(newRoom.algorithm != null) {
                        keyHandler.updateOutdatedKeys()
                    }
                }

                processEvents(
                    events = events,
                    prevBatch = room.prevBatch?.takeIf { room.timeline?.limited == true },
                    nextBatch = null,
                    currentBatch = INITIAL_BATCH,
                    roomId = room.id
                ).also { processedEvents ->
                    messages.addAll(processedEvents.messages)
                    /*withContext(Dispatchers.IO) {
                        roomMemberDao.insertAll(processedEvents.members)
                    }*/
                }
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
                    appendPing(AppPing(type = AppPingType.ConversationDashboard))
                }
            }
        }
    }

    private suspend fun getMembers(
        roomId: String,
        prevBatch: String?,
        ignore: Membership = Membership.LEAVE
    ): List<ClientEvent.RoomEvent.StateEvent<MemberEventContent>>? {
        return withContext(Dispatchers.IO) {
            val res = httpClient.safeRequest<GetMembers.Response> {
                get(urlString = "https://$homeserver/_matrix/client/v3/rooms/${roomId}/members") {
                    parameter("at", prevBatch)
                    parameter("not_membership", ignore)
                }
            }.success?.data?.chunk
            withContext(Dispatchers.Default) {
                res?.mapNotNull { event ->
                    @Suppress("UNCHECKED_CAST")
                    event.takeIf { it.content is MemberEventContent } as? ClientEvent.RoomEvent.StateEvent<MemberEventContent>
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
