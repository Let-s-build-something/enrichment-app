package ui.conversation.message

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationMessageIO
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.RoomMemberDao
import database.file.FileAccess
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.conversation.ConversationRepository
import ui.conversation.ConversationRoomSource
import ui.conversation.GetMessagesResponse
import ui.conversation.components.audio.MediaProcessorDataManager


class MessageDetailRepository(
    private val conversationMessageDao: ConversationMessageDao,
    private val roomMemberDao: RoomMemberDao,
    httpClient: HttpClient,
    fileAccess: FileAccess,
    conversationRoomDao: ConversationRoomDao,
    mediaDataManager: MediaProcessorDataManager
): ConversationRepository(
    httpClient = httpClient,
    conversationMessageDao = conversationMessageDao,
    mediaDataManager = mediaDataManager,
    fileAccess = fileAccess,
    roomMemberDao = roomMemberDao,
    conversationRoomDao = conversationRoomDao
) {

    /** Retrieves singular message from the local DB */
    suspend fun getMessage(id: String): ConversationMessageIO? {
        return withContext(Dispatchers.IO) {
            conversationMessageDao.get(id)?.also {
                it.user = getUser(id = it.authorPublicId)
            }
        }
    }

    private suspend fun getUser(id: String?): ConversationRoomMember? {
        return if(id == null) null else withContext(Dispatchers.IO) {
            roomMemberDao.get(userId = id)
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
                        conversationMessageDao.getAnchoredPaginated(
                            conversationId = conversationId,
                            anchorMessageId = anchorMessageId,
                            limit = config.pageSize,
                            offset = page * config.pageSize
                        ).let { res ->
                            GetMessagesResponse(
                                data = res,
                                hasNext = res.size == config.pageSize
                            )
                        }
                    },
                    getCount = {
                        certainMessageCount ?: conversationMessageDao.getCount(conversationId = conversationId).also {
                            certainMessageCount = it
                        }
                    },
                    size = config.pageSize
                ).also {
                    currentPagingSource = it
                }
            }
        )
    }
}