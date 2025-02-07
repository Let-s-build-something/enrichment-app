package data.shared

import augmy.interactive.shared.ui.base.currentPlatform
import base.utils.deviceName
import data.io.app.LocalSettings
import data.io.user.RequestInitApp
import data.io.user.UserIO
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
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
    suspend fun authenticateUser(
        localSettings: LocalSettings?,
        refreshToken: String? = null,
        expiresInMs: Long? = null
    ): UserIO? {
        return withContext(Dispatchers.IO) {
            if(Firebase.auth.currentUser != null) {
                httpClient.safeRequest<UserIO> {
                    post(urlString = "/api/v1/auth/init-app") {
                        setBody(
                            RequestInitApp(
                                fcmToken = localSettings?.fcmToken,
                                deviceName = deviceName() ?: currentPlatform.name,
                                refreshToken = refreshToken,
                                expiresInMs = expiresInMs
                            )
                        )
                    }
                }.success?.data?.also {
                    injectDemoData(it.publicId)
                } ?: UserIO()
            }else null
        }
    }

    //TODO remove DEMO data once not needed
    private suspend fun injectDemoData(publicId: String?) {
        withContext(Dispatchers.IO) {
            val conversationRoomDao: ConversationRoomDao = getKoin().get()
            val conversationMessageDao: ConversationMessageDao = getKoin().get()
            val networkItemDao: NetworkItemDao = getKoin().get()

            if(conversationRoomDao.getCount(publicId) == 0) {
                conversationRoomDao.insertAll(DemoData.demoRooms.map {
                    it.copy(
                        ownerPublicId = publicId
                    ).apply { batch = INITIAL_BATCH }
                })
                conversationMessageDao.insertAll(DemoData.demoMessages.onEach {
                    it.conversationId = DemoData.demoRooms.getOrNull(0)?.id
                })
                conversationMessageDao.insertAll(DemoData.demoMessages.onEach {
                    it.conversationId = DemoData.demoRooms.getOrNull(1)?.id
                })
                networkItemDao.insertAll(DemoData.proximityDemoData.map {
                    it.copy(ownerPublicId = publicId)
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