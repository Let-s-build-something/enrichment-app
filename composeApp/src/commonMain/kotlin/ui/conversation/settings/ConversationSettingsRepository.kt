package ui.conversation.settings

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import data.io.base.BaseResponse
import data.io.base.paging.MatrixPagingMetaIO
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessagesResponse
import data.io.user.NetworkItemIO
import data.shared.sync.DataSyncHandler
import data.shared.sync.MessageProcessor
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.RoomMemberDao
import database.file.FileAccess
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.model.users.Filters
import org.koin.mp.KoinPlatform
import ui.conversation.MediaRepository
import ui.conversation.components.audio.MediaProcessorDataManager
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class ConversationSettingsRepository(
    private val httpClient: HttpClient,
    private val roomMemberDao: RoomMemberDao,
    private val networkItemDao: NetworkItemDao,
    private val conversationRoomDao: ConversationRoomDao,
    private val matrixPagingDao: MatrixPagingMetaDao,
    mediaDataManager: MediaProcessorDataManager,
    fileAccess: FileAccess
): MediaRepository(httpClient, mediaDataManager, fileAccess)  {
    private val dataSyncHandler by lazy { KoinPlatform.getKoin().get<DataSyncHandler>() }
    private val json by lazy { KoinPlatform.getKoin().get<Json>() }

    private var currentPagingSource: PagingSource<*, *>? = null
    private var certainMembersCount: Int? = null

    private val requestMutex = Mutex()

    suspend fun updateRoom(room: ConversationRoomIO) = withContext(Dispatchers.IO) {
        conversationRoomDao.insert(room)
    }

    suspend fun getUser(
        userId: String,
        ownerPublicId: String?
    ): NetworkItemIO? = withContext(Dispatchers.IO) {
        networkItemDao.get(publicId = userId, ownerPublicId = ownerPublicId)
    }

    suspend fun removeRoom(
        conversationId: String,
        ownerPublicId: String?
    ) = withContext(Dispatchers.IO) {
        conversationRoomDao.remove(id = conversationId, ownerPublicId = ownerPublicId)
    }

    /** Returns a flow of conversation messages */
    fun getMembersListFlow(
        homeserver: () -> String,
        config: PagingConfig,
        conversationId: String? = null
    ): Pager<Int, ConversationRoomMember> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                val entityId = "members_$conversationId"

                ConversationMembersSource(
                    getMembers = { page ->
                        requestMutex.withLock {
                            if(conversationId == null) return@ConversationMembersSource GetMembersResponse(
                                data = listOf(), hasNext = false
                            )

                            withContext(Dispatchers.IO) {
                                val entity = matrixPagingDao.getByEntityId(entityId)
                                val prevBatch = entity?.prevBatch

                                roomMemberDao.getPaginated(
                                    roomId = conversationId,
                                    limit = config.pageSize,
                                    offset = page * config.pageSize
                                ).let { res ->
                                    if(res.isNotEmpty()) {
                                        GetMembersResponse(
                                            data = res,
                                            hasNext = res.size == config.pageSize
                                                    || prevBatch != null
                                                    || entity == null
                                        )
                                    }else null
                                } ?: if(prevBatch != null || entity == null) {
                                    getAndStoreNewMembers(
                                        limit = config.pageSize,
                                        conversationId = conversationId,
                                        fromBatch = prevBatch,
                                        homeserver = homeserver()
                                    )?.let { res ->
                                        certainMembersCount = certainMembersCount?.plus(res.messages.size)
                                        GetMembersResponse(
                                            data = res.members,
                                            hasNext = res.prevBatch != null && res.messages.isNotEmpty()
                                        ).also {
                                            val newPrevBatch = if(res.messages.isEmpty()
                                                && res.events == 0
                                                && res.members.isEmpty()
                                            ) null else res.prevBatch

                                            matrixPagingDao.insert(
                                                entity?.copy(prevBatch = newPrevBatch) ?: MatrixPagingMetaIO(
                                                    prevBatch = newPrevBatch,
                                                    entityId = entityId,
                                                    entityType = "members",
                                                    nextBatch = null
                                                )
                                            )
                                            if(newPrevBatch == null) {
                                                // we just downloaded empty page, let's refresh UI to end paging
                                                currentPagingSource?.invalidate()
                                            }
                                        }
                                    }
                                }else GetMembersResponse(listOf(), hasNext = false)
                            }
                        }
                    },
                    getCount = {
                        certainMembersCount ?: roomMemberDao.getCount(roomId = conversationId).also {
                            certainMembersCount = it
                        }
                    },
                    size = config.pageSize
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            }
        )
    }

    private suspend fun getAndStoreNewMembers(
        homeserver: String,
        fromBatch: String?,
        excludedUsers: Set<String>? = null,
        limit: Int,
        conversationId: String?
    ): MessageProcessor.SaveEventsResult? {
        return if(conversationId != null) {
            getMembers(
                limit = limit,
                conversationId = conversationId,
                fromBatch = fromBatch,
                excludedUsers = excludedUsers,
                homeserver = homeserver
            ).success?.data?.let { data ->
                dataSyncHandler.saveEvents(
                    events = data.chunk.orEmpty() + data.state.orEmpty(),
                    roomId = conversationId,
                    prevBatch = data.end
                )
            }
        }else null
    }

    /**
     * returns a list of network list
     * @param dir - direction of the list
     * @param fromBatch - The token to start returning events from, usually the "prev_batch" of the timeline.
     */
    private suspend fun getMembers(
        dir: String = "b",
        homeserver: String,
        fromBatch: String? = null,
        excludedUsers: Set<String>? = null,
        limit: Int,
        conversationId: String?
    ): BaseResponse<ConversationMessagesResponse> {
        return withContext(Dispatchers.IO) {
            if(conversationId == null) {
                return@withContext BaseResponse.Error()
            }

            httpClient.safeRequest<ConversationMessagesResponse> {
                get(
                    urlString = "https://${homeserver}/_matrix/client/v3/rooms/$conversationId/messages",
                    block =  {
                        parameter(
                            "filter",
                            json.encodeToString(
                                Filters.RoomFilter.RoomEventFilter(
                                    limit = limit.toLong(),
                                    lazyLoadMembers = true,
                                    rooms = setOf(conversationId),
                                    types = setOf("m.room.member")
                                )
                            )
                        )
                        parameter("dir", dir)
                        parameter("not_senders", excludedUsers)
                        parameter("include_redundant_members", false)
                        parameter("from", fromBatch)
                    }
                )
            }
        }
    }
}
