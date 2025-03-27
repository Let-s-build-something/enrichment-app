package ui.account.profile

import data.io.base.BaseResponse
import data.io.social.username.RequestUserPropertiesChange
import data.io.social.username.ResponseDisplayNameChange
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for making DB requests */
class DisplayNameChangeRepository(private val httpClient: HttpClient) {

    /** Makes a request to change username */
    suspend fun changeDisplayName(value: CharSequence): BaseResponse<ResponseDisplayNameChange> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ResponseDisplayNameChange> {
                patch(
                    urlString = "/api/v1/users",
                    block =  {
                        setBody(RequestUserPropertiesChange(displayName = value.toString()))
                    }
                )
            }
        }
    }

    /** Validates input value by user */
    suspend fun validateDisplayName(value: CharSequence): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<Any> {
                get(urlString = "/api/v1/users/validate/display-name?value=$value")
            }
        }
    }
}