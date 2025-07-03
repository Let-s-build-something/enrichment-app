package ui.conversation.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import base.utils.MediaType
import data.io.base.BaseResponse
import data.io.matrix.room.SearchRequest
import data.io.social.network.conversation.message.ConversationMessagesResponse
import data.io.social.network.conversation.message.FullConversationMessage
import data.shared.sync.DataSyncHandler
import data.shared.sync.MessageProcessor
import database.dao.ConversationMessageDao
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.clientserverapi.model.server.Search
import net.folivo.trixnity.clientserverapi.model.users.Filters
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import org.koin.mp.KoinPlatform
import ui.conversation.ConversationRoomSource
import ui.conversation.GetMessagesResponse
import ui.login.safeRequest
import utils.SharedLogger
import kotlin.collections.orEmpty
import kotlin.collections.plus

class ConversationSearchRepository(
    private val conversationMessageDao: ConversationMessageDao
) {
    private val httpClient by lazy { KoinPlatform.getKoin().get<HttpClient>() }
    private val dataSyncHandler by lazy { KoinPlatform.getKoin().get<DataSyncHandler>() }

    private val messageCount = hashMapOf<Pair<String, List<MediaType>>, Int>()
    private var currentPagingSource: PagingSource<*, *>? = null

    private suspend fun getQueryCount(
        query: String,
        selectedMediaTypes: List<MediaType>,
        conversationId: String?
    ): Int {
        return messageCount[query to selectedMediaTypes] ?: conversationMessageDao.countQueryPaginatedMimeType(
            conversationMessageDao.buildQueryPaginatedWithMimeTypes(
                query = query,
                conversationId = conversationId ?: "",
                mimeTypes = selectedMediaTypes.map { it.name.lowercase() },
                countOnly = true
            )
        ).also {
            messageCount[query to selectedMediaTypes] = it
        }
    }

    /** Attempts to invalidate local PagingSource with conversation messages */
    fun invalidateLocalSource() {
        currentPagingSource?.invalidate()
    }

    fun searchForMessages(
        query: () -> String,
        selectedMediaTypes: () -> List<MediaType>,
        homeserver: () -> String,
        config: PagingConfig,
        conversationId: String? = null
    ): Pager<Int, FullConversationMessage> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    getMessages = { page ->
                        if(conversationId.isNullOrBlank()) {
                            SharedLogger.logger.debug { "conversation id is null, can't load messages" }
                            return@ConversationRoomSource GetMessagesResponse()
                        } else if (query().isBlank() && selectedMediaTypes().isEmpty()) {
                            return@ConversationRoomSource GetMessagesResponse()
                        }
                        val totalItems = getQueryCount(query(), selectedMediaTypes(), conversationId)

                        withContext(Dispatchers.IO) {
                            conversationMessageDao.queryPaginatedMimeType(
                                conversationMessageDao.buildQueryPaginatedWithMimeTypes(
                                    query = query(),
                                    conversationId = conversationId,
                                    limit = config.pageSize,
                                    offset = page * config.pageSize,
                                    mimeTypes = selectedMediaTypes().map { it.name.lowercase() }
                                )
                            ).let { res ->
                                if(res.isNotEmpty()) {
                                    GetMessagesResponse(
                                        data = res,
                                        hasNext = (page * config.pageSize).plus(res.size) < totalItems
                                    )
                                }else null
                            } ?: queryRemoteEvents(
                                limit = config.pageSize,
                                conversationId = conversationId,
                                homeserver = homeserver(),
                                query = query(),
                                nextBatch =
                            )?.let { res ->
                                GetMessagesResponse(
                                    data = res.messages,
                                    hasNext = true // TODO implement stop to remote querying
                                )
                            } ?: GetMessagesResponse()
                        }
                    },
                    getCount = {
                        getQueryCount(query(), selectedMediaTypes(), conversationId)
                    },
                    size = config.pageSize
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            }
        )
    }

    private suspend fun queryRemoteEvents(
        homeserver: String,
        limit: Int,
        query: String,
        nextBatch: String?,
        conversationId: String
    ): MessageProcessor.SaveEventsResult? {
        return getMessages(
            limit = limit,
            conversationId = conversationId,
            query = query,
            nextBatch = nextBatch,
            homeserver = homeserver
        ).success?.data?.searchCategories?.roomEvents?.let { data ->
            val results = data.results?.flatMap { res ->
                val events = mutableListOf<RoomEvent<*>>().apply {
                    res.context?.eventsBefore?.let { addAll(it) }
                    res.context?.eventsAfter?.let { addAll(it) }
                    res.result?.let { add(it) }
                }

                if (events.isNotEmpty()) {
                    dataSyncHandler.saveEvents(
                        events = events,
                        roomId = conversationId,
                        prevBatch = res.context?.start,
                        nextBatch = res.context?.end
                    ).messages
                }else listOf()
            }
            MessageProcessor.SaveEventsResult(
                messages = results.orEmpty(),
                prevBatch = data.nextBatch,
                events = results?.size ?: 0,
                members = listOf(),
                changeInMessages = !results.isNullOrEmpty()
            )
        }
    }

    /**
     * returns a list of network list
     * @param nextBatch - The point to return events from. If given, this should be a next_batch result from a previous call to this endpoint.
     */
    private suspend fun getMessages(
        homeserver: String,
        query: String,
        nextBatch: String? = null,
        limit: Int,
        conversationId: String?
    ): BaseResponse<Search.Response> {
        return withContext(Dispatchers.IO) {
            if(conversationId == null) {
                return@withContext BaseResponse.Error()
            }

            httpClient.safeRequest<Search.Response> {
                post(
                    urlString = "https://${homeserver}/_matrix/client/v3/search",
                    block =  {
                        parameter("next_batch", nextBatch)
                        setBody(
                            SearchRequest(
                                searchCategories = SearchRequest.Categories(
                                    roomEvents = SearchRequest.Categories.RoomEventsCriteria(
                                        searchTerm = query,
                                        eventContext = SearchRequest.Categories.RoomEventsCriteria.IncludeEventContext(
                                            afterLimit = 10,
                                            beforeLimit = 10,
                                            includeProfile = false
                                        ),
                                        filter = SearchRequest.Categories.RoomEventsFilter(
                                            rooms = setOf(conversationId),
                                            types = setOf("m.room.message", "m.reaction", "m.room.name"),
                                            lazyLoadMembers = true,
                                            limit = limit,
                                            includeRedundantMembers = false
                                        ),
                                        includeState = false,
                                        orderBy = SearchRequest.Categories.RoomEventsCriteria.Ordering.Recent,
                                    )
                                )
                            )
                        )
                    }
                )
            }
        }
    }
}
