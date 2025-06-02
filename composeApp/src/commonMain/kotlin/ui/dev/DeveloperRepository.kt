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
import org.koin.mp.KoinPlatform
import ui.login.safeRequest

class DeveloperRepository {

    private val httpClient by lazy {
        KoinPlatform.getKoin().get<HttpClient>()
    }

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
        with(KoinPlatform.getKoin()) {
            get<NetworkItemDao>().removeAll()
            get<ConversationMessageDao>().removeAll()
            get<EmojiSelectionDao>().removeAll()
            get<PagingMetaDao>().removeAll()
            get<ConversationRoomDao>().removeAll()
            get<PresenceEventDao>().removeAll()
            get<MatrixPagingMetaDao>().removeAll()
        }
        secureSettings.clear(force = true)
    }
}