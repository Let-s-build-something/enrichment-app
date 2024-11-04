package ui.account

import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.UserConfiguration
import data.shared.SharedRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class AccountDashboardRepository(private val httpClient: HttpClient): SharedRepository(httpClient) {

    /** Makes a request to change user's privacy setting */
    suspend fun changeUserConfiguration(configuration: UserConfiguration): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.post(
                urlString = "/v1/social/configuration",
                block =  {
                    setBody(configuration)
                }
            ).getResponse<Any>().also {
                println(it.toString())
            }
        }
    }
}