package ui.login.username

import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.username.RequestUsernameChange
import data.io.social.username.ResponseUsernameChange
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for making DB requests */
class UsernameChangeRepository(private val httpClient: HttpClient) {

    /** Makes a request to change username */
    suspend fun changeUsername(username: String): BaseResponse<ResponseUsernameChange> {
        return withContext(Dispatchers.IO) {
            httpClient.post(
                urlString = "v1/social/username",
                block =  {
                    setBody(RequestUsernameChange(username))
                }
            ).getResponse<ResponseUsernameChange>()
        }
    }
}