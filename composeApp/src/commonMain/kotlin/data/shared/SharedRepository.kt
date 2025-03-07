package data.shared

import augmy.interactive.shared.ui.base.currentPlatform
import base.utils.deviceName
import data.io.app.LocalSettings
import data.io.user.RequestInitApp
import data.io.user.UserIO
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
                val res = httpClient.safeRequest<UserIO> {
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
                }.success?.data
                // BE can send empty strings
                res?.copy(
                    matrixUserId = res.matrixUserId.takeIf { !it.isNullOrBlank() },
                    accessToken = res.accessToken.takeIf { !it.isNullOrBlank() },
                )
            }else null
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