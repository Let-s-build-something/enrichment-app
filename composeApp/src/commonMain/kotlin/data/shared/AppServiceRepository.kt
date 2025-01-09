package data.shared

import data.io.base.BaseResponse
import data.io.user.RequestUpdateFcmToken
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class AppServiceRepository(private val httpClient: HttpClient) {

    /** Makes a request to update an FCM token */
    suspend fun updateFCMToken(
        prevFcmToken: String?,
        publicId: String?,
        newToken: String
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest {
                put(urlString = "/api/v1/users/${publicId}/fcm-tokens") {
                    setBody(
                        RequestUpdateFcmToken(
                            fcmToken = newToken,
                            oldFcmToken = prevFcmToken
                        )
                    )
                }
            }
        }
    }
}
