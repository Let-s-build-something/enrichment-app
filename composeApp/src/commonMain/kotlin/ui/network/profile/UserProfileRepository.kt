package ui.network.profile

import data.io.base.BaseResponse
import data.io.social.network.request.CircleRequestResponse
import data.io.social.network.request.CirclingRequest
import data.io.user.PublicUserProfileIO
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class UserProfileRepository(private val httpClient: HttpClient) {

    /** Makes a request to get a user */
    suspend fun getUserProfile(publicId: String): BaseResponse<PublicUserProfileIO> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<PublicUserProfileIO> {
                get(urlString = "/api/v1/users/${publicId}")
            }
        }
    }

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