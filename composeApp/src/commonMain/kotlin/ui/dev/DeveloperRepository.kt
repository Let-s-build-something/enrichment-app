package ui.dev

import data.io.base.BaseResponse
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.EmojiSelectionDao
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import koin.secureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

class DeveloperRepository(
    private val networkItemDao: NetworkItemDao,
    private val conversationMessageDao: ConversationMessageDao,
    private val emojiSelectionDao: EmojiSelectionDao,
    private val pagingMetaDao: PagingMetaDao,
    private val conversationRoomDao: ConversationRoomDao,
    private val presenceEventDao: PresenceEventDao,
    private val matrixPagingMetaDao: MatrixPagingMetaDao,
    private val httpClient: HttpClient
) {

    suspend fun postStreamData(
        url: String,
        body: String
    ): BaseResponse<Any> = withContext(Dispatchers.IO) {
        httpClient.safeRequest<Any> {
            httpClient.post(url = Url(url)) {
                setBody(body)
            }
        }
    }

    suspend fun clearAllDao() {
        networkItemDao.removeAll()
        conversationMessageDao.removeAll()
        emojiSelectionDao.removeAll()
        conversationRoomDao.removeAll()
        presenceEventDao.removeAll()
        pagingMetaDao.removeAll()
        matrixPagingMetaDao.removeAll()
        secureSettings.clear(force = true)
    }
}