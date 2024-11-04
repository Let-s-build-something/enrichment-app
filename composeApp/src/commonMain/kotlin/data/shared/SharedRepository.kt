package data.shared

import augmy.interactive.shared.ui.base.currentPlatform
import data.io.app.LocalSettings
import data.io.base.BaseResponse.Companion.getResponse
import data.io.user.RequestGetUser
import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

open class SharedRepository(private val httpClient: HttpClient) {

    /** Makes a request to create a user */
    suspend fun authenticateUser(localSettings: LocalSettings?): UserIO? {
        return withContext(Dispatchers.IO) {
            if(Firebase.auth.currentUser != null) {
                httpClient.post(urlString = "/v1/auth/init-app") {
                    setBody(
                        RequestGetUser(
                            fcmToken = localSettings?.fcmToken,
                            platform = currentPlatform
                        )
                    )
                }.getResponse<UserIO>().also {
                    println(it.toString())
                }.success?.data ?: UserIO()
            }else null
        }
    }
}

/** sets URL parameters for paging */
fun HttpRequestBuilder.setPaging(
    page: Int,
    size: Int = 20
) = this.apply {
    parameters {
        append("page", page.toString())
        append("size", size.toString())
    }
}