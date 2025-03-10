package ui.conversation.message

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.user.NetworkItemIO
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
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
    conversationRoomDao: ConversationRoomDao,
    mediaDataManager: MediaProcessorDataManager
): ConversationRepository(
    httpClient = httpClient,
    conversationMessageDao = conversationMessageDao,
    mediaDataManager = mediaDataManager,
    fileAccess = fileAccess,
    networkItemDao = networkItemDao,
    conversationRoomDao = conversationRoomDao
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

    private var lastBatch: String? = null

    /** Returns a flow of conversation reply messages */
    fun getMessagesListFlow(
        config: PagingConfig,
        conversationId: String? = null,
        anchorMessageId: String? = null
    ): Pager<String, ConversationMessageIO> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    getMessages = { batch ->
                        conversationMessageDao.getAnchoredPaginated(
                            conversationId = conversationId,
                            anchorMessageId = anchorMessageId,
                            batch = batch
                        ).also {
                            if(it.isEmpty()) {
                                lastBatch = batch
                                invalidateLocalSource()
                            }
                        }
                    },
                    findPreviousBatch = { currentBatch ->
                        conversationMessageDao.getPreviousBatch(
                            conversationId = conversationId,
                            currentBatch = currentBatch
                        )
                    },
                    countItems = { batch ->
                        conversationMessageDao.getCount(
                            conversationId = conversationId,
                            batch = batch
                        )
                    },
                    size = config.pageSize,
                    lastBatch = lastBatch
                ).also {
                    currentPagingSource = it
                }
            }
        )
    }
}