package ui.conversation.message

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.base.paging.PaginationInfo
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import data.io.user.NetworkItemIO
import database.dao.ConversationMessageDao
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import database.file.FileAccess
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.conversation.ConversationRepository
import ui.conversation.ConversationRoomSource
import ui.conversation.components.audio.MediaProcessorDataManager


class MessageDetailRepository(
    private val conversationMessageDao: ConversationMessageDao,
    private val networkItemDao: NetworkItemDao,
    httpClient: HttpClient,
    fileAccess: FileAccess,
    mediaDataManager: MediaProcessorDataManager,
    pagingMetaDao: PagingMetaDao
): ConversationRepository(
    httpClient = httpClient,
    conversationMessageDao = conversationMessageDao,
    pagingMetaDao = pagingMetaDao,
    mediaDataManager = mediaDataManager,
    fileAccess = fileAccess
) {

    /** Retrieves singular message from the local DB */
    suspend fun getMessage(id: String, ownerPublicId: String?): ConversationMessageIO? {
        return withContext(Dispatchers.IO) {
            conversationMessageDao.get(id)?.also {
                it.user = getUser(
                    id = it.authorPublicId,
                    ownerPublicId = ownerPublicId
                )
            }
        }
    }

    /** Retrieves singular message from the local DB */
    private suspend fun getUser(
        id: String?,
        ownerPublicId: String?
    ): NetworkItemIO? {
        return if(id == null) null else withContext(Dispatchers.IO) {
            networkItemDao.get(publicId = id, ownerPublicId = ownerPublicId)
        }
    }

    /** Returns a flow of conversation reply messages */
    fun getMessagesListFlow(
        config: PagingConfig,
        conversationId: String? = null,
        anchorMessageId: String? = null
    ): Pager<Int, ConversationMessageIO> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    getMessages = { page ->
                        val res = conversationMessageDao.getAnchoredPaginated(
                            conversationId = conversationId,
                            anchorMessageId = anchorMessageId,
                            limit = config.pageSize,
                            offset = page * config.pageSize
                        )

                        BaseResponse.Success(
                            ConversationMessagesResponse(
                                content = res,
                                pagination = PaginationInfo(
                                    page = page,
                                    size = res.size,
                                    totalItems = conversationMessageDao.getCount(conversationId)
                                )
                            )
                        )
                    },
                    size = config.pageSize
                ).also {
                    currentPagingSource = it
                }
            }
        )
    }
}