package ui.network.add_new

import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.network.request.CircleRequestResponse
import data.io.social.network.request.CirclingRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class NetworkAddNewRepository(private val httpClient: HttpClient) {

    /** Acts upon a circling request */
    suspend fun includeNewUser(action: CirclingRequest): BaseResponse<CircleRequestResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<CircleRequestResponse> {
                post(
                    urlString = "/api/v1/social/network/requests",
                    block =  {
                        setBody(action)
                    }
                )
            }
        }
    }
}