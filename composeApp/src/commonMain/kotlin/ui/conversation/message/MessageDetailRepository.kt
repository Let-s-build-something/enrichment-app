package ui.conversation.message

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.social.network.conversation.message.FullConversationMessage
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.MessageReactionDao
import database.dao.RoomMemberDao
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
    roomMemberDao: RoomMemberDao,
    httpClient: HttpClient,
    fileAccess: FileAccess,
    conversationRoomDao: ConversationRoomDao,
    messageReactionDao: MessageReactionDao,
    mediaDataManager: MediaProcessorDataManager
): ConversationRepository(
    httpClient = httpClient,
    conversationMessageDao = conversationMessageDao,
    mediaDataManager = mediaDataManager,
    fileAccess = fileAccess,
    roomMemberDao = roomMemberDao,
    conversationRoomDao = conversationRoomDao,
    messageReactionDao = messageReactionDao
) {

    /** Retrieves singular message from the local DB */
    suspend fun getMessage(id: String): FullConversationMessage? {
        return withContext(Dispatchers.IO) {
            conversationMessageDao.get(id)
        }
    }

    /** Returns a flow of conversation reply messages */
    fun getMessagesListFlow(
        config: PagingConfig,
        conversationId: String? = null,
        anchorMessageId: String? = null
    ): Pager<Int, FullConversationMessage> {
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