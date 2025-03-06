package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomSummary
import data.io.matrix.room.event.ConversationRoomMember
import data.io.matrix.room.event.content.processEvents
import data.io.social.UserVisibility
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.user.PresenceData
import data.shared.SharedDataManager
import data.shared.SharedRepository
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomMemberDao
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import korlibs.io.async.onCancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.model.rooms.GetMembers
import net.folivo.trixnity.clientserverapi.model.sync.Sync
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.model.events.stateKeyOrNull
import net.folivo.trixnity.crypto.olm.OlmEventHandler
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.conversation.ConversationRoomSource.Companion.INITIAL_BATCH
import ui.login.safeRequest
import kotlin.time.Duration.Companion.milliseconds

internal val dataSyncModule = module {
    factory { DataSyncService() }
    single { DataSyncService() }
}

class DataSyncService {
    companion object {
        const val SYNC_INTERVAL = 60_000L
        private const val START_ANEW = false
    }

    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var homeserver: String? = null
    private var nextBatch: String? = null
    private var isRunning = false
    private var handler: DataSyncHandler? = null

    /** Begins the synchronization process and runs it over and over as long as the app is running or stopped via [stop] */
    fun sync(homeserver: String, delay: Long? = null) {
        println("kostka_test, sync, isRunning: $isRunning, homeserver: $homeserver")
        if(!isRunning) {
            this.homeserver = homeserver
            isRunning = true
            syncScope.launch {
                this.coroutineContext.onCancel {
                    isRunning = false
                }

                sharedDataManager.matrixClient?.let { client ->
                    delay?.let { delay(it) }
                    (sharedDataManager.currentUser.value?.takeIf { it.publicId != null } ?: sharedRepository.authenticateUser(
                        localSettings = sharedDataManager.localSettings.value
                    )).let {
                        sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(it) ?: it
                        handler = DataSyncHandler(homeserver = homeserver)
                        KoinPlatform.getKoin().getOrNull<OlmEventHandler>()?.startInCoroutineScope(this)
                        enqueue(client = client)
                    }
                }
            }
        }
    }

    fun stop() {
        if(isRunning) {
            isRunning = false
            handler = null
            syncScope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun CoroutineScope.enqueue(
        client: MatrixClient,
        homeserver: String? = this@DataSyncService.homeserver,
        since: String? = this@DataSyncService.nextBatch
    ) {
        val owner = sharedDataManager.currentUser.value?.matrixUserId ?: return
        if(homeserver == null) return

        val initialEntity = if(START_ANEW) null else matrixPagingMetaDao.getByEntityId(
            entityId = "${homeserver}_$owner"
        )
        var prevBatch: String? = initialEntity?.currentBatch
        var currentBatch = since ?: initialEntity?.nextBatch

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
                since ?: if(START_ANEW) null else matrixPagingMetaDao.getByEntityId(
                    entityId = "${homeserver}_$owner"
                )?.nextBatch
            },
            setBatchToken = { nextBatch ->
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

                prevBatch = "$currentBatch".takeIf { currentBatch != null }
                currentBatch = nextBatch
            }
        )

        client.api.sync.subscribe {
            handler?.handle(
                client = client,
                response = it.syncResponse,
                prevBatch = prevBatch,
                owner = owner
            )
        }
    }
}

class DataSyncHandler(private val homeserver: String) {

    companion object {
        private const val PING_EXPIRY_MS = 60_000 * 15
    }

    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()

    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()
    private val roomMemberDao: RoomMemberDao by KoinPlatform.getKoin().inject()
    private val presenceEventDao: PresenceEventDao by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val keyVerificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun verifyKeys(client: MatrixClient) {
        keyVerificationScope.launch {
            client.verification.getSelfVerificationMethods().collectLatest { verificationMethods ->
                println("kostka_test, selfVerificationMethods: $verificationMethods")

                when(verificationMethods) {
                    is VerificationService.SelfVerificationMethods.NoCrossSigningEnabled -> {
                        client.key.bootstrapCrossSigning()
                    }
                    is VerificationService.SelfVerificationMethods.PreconditionsNotMet -> {
                        // TODO?
                    }
                    is VerificationService.SelfVerificationMethods.CrossSigningEnabled -> {
                        // super!
                    }
                    else -> {}
                }
            }
        }
    }

    suspend fun handle(
        client: MatrixClient,
        response: Sync.Response,
        prevBatch: String?,
        owner: String
    ) {
        verifyKeys(client = client)

        withContext(Dispatchers.Default) {
            val matrixRooms = response.room?.let { matrixRooms ->
                mutableListOf<ConversationRoomIO>().apply {
                    addAll(matrixRooms.join?.map { it.value.asConversation(id = it.key) }.orEmpty())
                    addAll(matrixRooms.invite?.map { it.value.asConversation(id = it.key) }.orEmpty())
                    addAll(matrixRooms.knock?.map { it.value.asConversation(id = it.key) }.orEmpty())
                    addAll(matrixRooms.leave?.map { it.value.asConversation(id = it.key) }.orEmpty())
                }
            }
            val messages = mutableListOf<ConversationMessageIO>()
            val rooms = mutableListOf<ConversationRoomIO>()
            val presenceContent = mutableListOf<PresenceData>()

            println("kostka_test, toDevice: ${response.toDevice}, deviceLists: ${response.deviceLists}")

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

                // =======================  The room is saved, it's safe to proceed  ========================= //
                // ensure we have all the members
                if(events.isNotEmpty()) {
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
                    }
                }

                processEvents(
                    client = client,
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
