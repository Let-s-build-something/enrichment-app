package ui.conversation.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import base.utils.MediaType
import data.io.social.network.conversation.message.FullConversationMessage
import data.shared.sync.MessageProcessor
import database.dao.ConversationMessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.conversation.ConversationRoomSource
import ui.conversation.GetMessagesResponse
import utils.SharedLogger

class ConversationSearchRepository(
    private val conversationMessageDao: ConversationMessageDao
) {
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
                                homeserver = homeserver()
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
        conversationId: String
    ): MessageProcessor.SaveEventsResult? {
        // TODO: implement remote events
        return null
    }
}
