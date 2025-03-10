package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomSummary
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.UserVisibility
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import data.io.user.PresenceData
import data.shared.SharedDataManager
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomMemberDao
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.room.decrypt
import net.folivo.trixnity.client.roomEventEncryptionServices
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.model.sync.Sync
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedToDeviceEventContent.OlmEncryptedToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.model.events.stateKeyOrNull
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.conversation.ConversationRoomSource.Companion.INITIAL_BATCH
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private var handler: DataSyncHandler? = null

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
                    sharedDataManager.currentUser.value?.takeIf { it.tag != null }?.let {
                        sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(it) ?: it
                        handler = DataSyncHandler()
                        enqueue(client = client)
                    } ?: stop().also {
                        println("kostka_test, sync stop, user tag is null")
                    }
                } ?: stop().also {
                    println("kostka_test, sync stop, client is null")
                }
            }
        }
    }

    fun stop() {
        if(isRunning) {
            handler?.stop()
            println("kostka_test, stopping sync")
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
            nextBatch = it.syncResponse.nextBatch

            handler?.handle(
                client = client,
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
                (since ?: matrixPagingMetaDao.getByEntityId(entityId = "${homeserver}_$owner")?.nextBatch)
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

                prevBatch = currentBatch
                currentBatch = nextBatch
            }
        )
    }
}

class DataSyncHandler {

    companion object {
        private const val PING_EXPIRY_MS = 60_000 * 15
    }

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

    fun stop() {
        syncScope.coroutineContext.cancelChildren()
        decryptionScope.coroutineContext.cancelChildren()
        keyVerificationScope.coroutineContext.cancelChildren()
    }

    suspend fun handle(
        client: MatrixClient,
        response: Sync.Response,
        nextBatch: String?,
        currentBatch: String,
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
                )
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
                        if(event is RoomEvent.StateEvent) {
                            (event as? RoomEvent.StateEvent<MemberEventContent>)?.let {
                                memberUpdates.add(it)
                            }
                        }
                        messages.add(
                            ConversationMessageIO(
                                content = "${content.membership} ${content.displayName}",
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
                    is ReceiptEventContent -> {
                        (event as? ClientEvent<ReceiptEventContent>)?.let {
                            receipts.add(it)
                        }
                        messages.add(
                            ConversationMessageIO(
                                sentAt = event.originTimestampOrNull?.let { millis ->
                                    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                                },
                                conversationId = roomId,
                                authorPublicId = event.senderOrNull?.full,
                                id = event.idOrNull?.full ?: Uuid.random().toString(),
                                state = MessageState.Sent,
                                currentBatch = currentBatch,
                                prevBatch = prevBatch,
                                nextBatch = nextBatch
                            )
                        )
                    }
                    is OlmEncryptedToDeviceEventContent, is MegolmEncryptedMessageEventContent -> {
                        if(event is RoomEvent.MessageEvent) {
                            decryptEvent(event)
                        }
                        messages.add(
                            ConversationMessageIO(
                                content = "MESSAGE FAILED TO BE DECRYPTED",
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

                if(messages.isNotEmpty()) {
                    appendPing(
                        AppPing(
                            type = AppPingType.Conversation,
                            identifiers = messages.mapNotNull { it.conversationId }.distinct()
                        )
                    )
                }
            }

            ProcessedEvents(
                messages = messages,
                members = members
            )
        }
    }

    private val decryptionScope = CoroutineScope(Dispatchers.Default)
    private fun decryptEvent(event: RoomEvent.MessageEvent<*>) {
        decryptionScope.launch {
            sharedDataManager.matrixClient.value?.roomEventEncryptionServices?.decrypt(event).also { decryptedEvent ->
                println("kostka_test, decrypted megolm event: ${decryptedEvent?.getOrThrow()}, event: $event")
            }
            println("kostka_test, decrypting megolm decrypted finished")
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
