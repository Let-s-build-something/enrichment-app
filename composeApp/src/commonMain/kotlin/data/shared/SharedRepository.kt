package data.shared

import data.io.app.LocalSettings
import data.io.user.RequestGetUser
import data.io.user.UserIO
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ParametersBuilder
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import ui.home.HomeRepository.Companion.INITIAL_BATCH
import ui.login.safeRequest

open class SharedRepository(private val httpClient: HttpClient) {

    /** Makes a request to create a user */
    suspend fun authenticateUser(localSettings: LocalSettings?): UserIO? {
        return withContext(Dispatchers.IO) {
            if(Firebase.auth.currentUser != null) {
                httpClient.safeRequest<UserIO> {
                    post(urlString = "/api/v1/auth/init-app") {
                        setBody(
                            RequestGetUser(fcmToken = localSettings?.fcmToken)
                        )
                    }
                }.success?.data?.also {
                    injectDemoData(Firebase.auth.currentUser?.uid)
                } ?: UserIO()
            }else null
        }
    }

    //TODO remove DEMO data once not needed
    private suspend fun injectDemoData(publicId: String?) {
        withContext(Dispatchers.IO) {
            val conversationRoomDao: ConversationRoomDao = getKoin().get()
            val conversationMessageDao: ConversationMessageDao = getKoin().get()

            if(conversationRoomDao.getCount(publicId) == 0) {
                conversationRoomDao.insertAll(DemoData.demoRooms.onEach { room ->
                    room.batch = INITIAL_BATCH
                    room.ownerPublicId = publicId
                })
                conversationMessageDao.insertAll(DemoData.demoMessages.onEach {
                    it.conversationId = DemoData.demoRooms.getOrNull(0)?.id
                })
                conversationMessageDao.insertAll(DemoData.demoMessages.onEach {
                    it.conversationId = DemoData.demoRooms.getOrNull(1)?.id
                })
            }
        }
    }
}

object ApiConstants {
    /** Url of the GIPHY API */
    const val GIPHY_API_URL = "https://api.giphy.com"
}

/** sets URL parameters for paging */
fun HttpRequestBuilder.setPaging(
    page: Int,
    size: Int = 20,
    builder: ParametersBuilder.() -> Unit = {}
) = this.apply {
    parameters {
        append("page", page.toString())
        append("size", size.toString())
        builder()
    }
}