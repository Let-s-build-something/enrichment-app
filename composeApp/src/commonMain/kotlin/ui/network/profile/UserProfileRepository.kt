package ui.network.profile

import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class UserProfileRepository(private val httpClient: HttpClient) {

    /** Makes a request to get a user */
    suspend fun getUserProfile(publicId: String): BaseResponse<NetworkItemIO> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<NetworkItemIO> {
                get(urlString = "/api/v1/users/${publicId}")
            }
        }
    }
}