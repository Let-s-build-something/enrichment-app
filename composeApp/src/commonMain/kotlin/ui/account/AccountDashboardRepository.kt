package ui.account

import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.UserConfiguration
import data.shared.SharedRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class AccountDashboardRepository(private val httpClient: HttpClient): SharedRepository(httpClient) {

    /** Makes a request to change user's privacy setting */
    suspend fun changeUserConfiguration(configuration: UserConfiguration): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<Any> {
                patch(
                    urlString = "/api/v1/social/configurations",
                    block =  {
                        setBody(configuration)
                    }
                )
            }
        }
    }
}