package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomSummary
import data.io.social.UserVisibility
import data.io.social.network.conversation.message.MediaIO
import data.io.user.PresenceData
import data.shared.SharedDataManager
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
import korlibs.io.async.onCancel
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
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.model.sync.Sync
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.conversation.ConversationRoomSource.Companion.INITIAL_BATCH
import kotlin.time.Duration.Companion.milliseconds

internal val dataSyncModule = module {
    factory { DataSyncService() }
    single { DataSyncService() }
}

class DataSyncService {
    companion object {
        const val SYNC_INTERVAL = 60_000L
        private const val START_ANEW = true
    }

    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var homeserver: String? = null
    private var nextBatch: String? = null
    private var isRunning = false
    private val handler = DataSyncHandler()

    /** Begins the synchronization process and runs it over and over as long as the app is running or stopped via [stop] */
    fun sync(homeserver: String, delay: Long? = null) {
        if(!isRunning) {
            this.homeserver = homeserver
            isRunning = true
            syncScope.launch {
                if(START_ANEW) {
                    matrixPagingMetaDao.removeAll()
                }

                this.coroutineContext.onCancel {
                    isRunning = false
                }

                sharedDataManager.matrixClient.value?.let { client ->
                    delay?.let { delay(it) }
                    if(sharedDataManager.currentUser.value?.isFullyValid == true) {
                        enqueue(client = client)
                    }else stop()
                } ?: stop()
            }
        }
    }

    fun stop() {
        if(isRunning) {
            handler.stop()
            println("kostka_test, stopping sync")
            isRunning = false
            syncScope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun CoroutineScope.enqueue(
        client: MatrixClient,
        homeserver: String? = this@DataSyncService.homeserver,
        since: String? = this@DataSyncService.nextBatch
    ) {
        val owner = sharedDataManager.currentUser.value?.matrixUserId
        if(homeserver == null || owner == null) {
            println("kostka_test, enqueue stop, owner: $owner")
            stop()
            return
        }

        val initialEntity = matrixPagingMetaDao.getByEntityId(
            entityId = "${homeserver}_$owner"
        )
        var prevBatch: String? = initialEntity?.currentBatch
        var currentBatch = since ?: initialEntity?.nextBatch ?: INITIAL_BATCH

        client.api.sync.subscribe {
            println("kostka_test, new sync data, activeDeviceVerification: ${
                client.verification.activeDeviceVerification.value?.state?.value
            }")
            handler.handle(
                response = it.syncResponse,
                nextBatch = nextBatch,
                currentBatch = currentBatch,
                owner = owner
            )
        }

        client.api.sync.start(
            filter = null,
            timeout = SYNC_INTERVAL.milliseconds,
            asUserId = UserId(owner),
            setPresence = when(sharedDataManager.currentUser.value?.configuration?.visibility) {
                UserVisibility.Online -> Presence.ONLINE
                UserVisibility.Invisible, UserVisibility.Offline -> Presence.OFFLINE
                else -> Presence.UNAVAILABLE
            },
            scope = this,
            getBatchToken = {
                (since ?: nextBatch ?: matrixPagingMetaDao.getByEntityId(entityId = "${homeserver}_$owner")?.nextBatch)
            },
            setBatchToken = { nextBatch ->
                this@DataSyncService.nextBatch = nextBatch
                matrixPagingMetaDao.insert(
                    MatrixPagingMetaIO(
                        entityId = "${homeserver}_$owner",
                        entityType = PagingEntityType.Sync.name,
                        nextBatch = nextBatch,
                        currentBatch = currentBatch,
                        // after getting a response, the currentBatch becomes prevBatch
                        prevBatch = prevBatch
                    )
                )

                prevBatch = currentBatch
                currentBatch = nextBatch
            }
        )
    }
}

internal class DataSyncHandler: MessageProcessor() {

    companion object {
        private const val PING_EXPIRY_MS = 60_000 * 15
    }

    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val presenceEventDao: PresenceEventDao by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun stop() {
        syncScope.coroutineContext.cancelChildren()
        decryptionScope.coroutineContext.cancelChildren()
    }

    suspend fun handle(
        response: Sync.Response,
        nextBatch: String?,
        currentBatch: String,
        owner: String
    ) {
        withContext(Dispatchers.Default) {
            val matrixRooms = response.room?.let { matrixRooms ->
                mutableListOf<ConversationRoomIO>().apply {
                    addAll(matrixRooms.join?.map { it.value.asConversation(id = it.key) }.orEmpty())
                    addAll(matrixRooms.invite?.map { it.value.asConversation(id = it.key) }.orEmpty())
                    addAll(matrixRooms.knock?.map { it.value.asConversation(id = it.key) }.orEmpty())
                    addAll(matrixRooms.leave?.map { it.value.asConversation(id = it.key) }.orEmpty())
                }
            }
            val rooms = mutableListOf<ConversationRoomIO>()
            val presenceContent = mutableListOf<PresenceData>()

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
                                is RoomEvent.MessageEvent -> {
                                    this.copy(roomId = roomId.takeIf { it.full.isNotBlank() } ?: RoomId(room.id))
                                }
                                is RoomEvent.StateEvent -> {
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
                        canonicalAlias = alias ?: name
                    ),
                    prevBatch = room.timeline?.previousBatch,
                    ownerPublicId = owner,
                    historyVisibility = historyVisibility,
                    primaryKey = "${room.id}_${owner}",
                    algorithm = algorithm?.algorithm
                )

                // either update existing one, or insert new one
                (conversationRoomDao.getItem(
                    id = room.id,
                    ownerPublicId = owner
                )?.update(newItem) ?: newItem).also { newRoom ->
                    conversationRoomDao.insert(newRoom)
                    rooms.add(newRoom)
                }

                processEvents(
                    events = events,
                    prevBatch = newItem.prevBatch?.takeIf { room.timeline?.limited == true },
                    nextBatch = nextBatch,
                    currentBatch = currentBatch,
                    roomId = newItem.id
                ).also {
                    if(it.messages.isNotEmpty()) {
                        appendPing(
                            AppPing(
                                type = AppPingType.Conversation,
                                identifiers = it.messages.mapNotNull { it.conversationId }.distinct()
                            )
                        )
                    }
                }
            }

            // Save presence locally
            withContext(Dispatchers.IO) {
                if(presenceContent.isNotEmpty()) {
                    presenceEventDao.insertAll(presenceContent)
                }
            }

            withContext(Dispatchers.IO) {
                if(rooms.isNotEmpty()) {
                    appendPing(AppPing(type = AppPingType.ConversationDashboard))
                }
            }
        }
    }

    private fun Sync.Response.Rooms.JoinedRoom.asConversation(id: RoomId) = ConversationRoomIO(
        id = id.full,
        unreadNotifications = unreadNotifications,
        summary = RoomSummary(
            heroes = summary?.heroes,
            joinedMemberCount = summary?.joinedMemberCount?.toInt(),
            invitedMemberCount = summary?.invitedMemberCount?.toInt()
        )
    ).also {
        it.state = state
        it.timeline = timeline
        it.accountData = accountData
        it.ephemeral = ephemeral
    }

    private fun Sync.Response.Rooms.KnockedRoom.asConversation(id: RoomId) = ConversationRoomIO(
        id = id.full,
        knockState = knockState
    )

    private fun Sync.Response.Rooms.InvitedRoom.asConversation(id: RoomId) =  ConversationRoomIO(
        id = id.full,
        inviteState = inviteState
    )

    private fun Sync.Response.Rooms.LeftRoom.asConversation(id: RoomId) = ConversationRoomIO(
        id = id.full
    ).also {
        it.state = state
        it.timeline = timeline
        it.accountData = accountData
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
