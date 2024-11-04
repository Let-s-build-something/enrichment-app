package ui.login

import data.io.base.BaseResponse.Companion.getResponse
import data.io.user.RequestCreateUser
import data.io.user.ResponseCreateUser
import data.shared.SharedRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class LoginRepository(private val httpClient: HttpClient): SharedRepository(httpClient) {

    /** Makes a request to create a user */
    suspend fun createUser(data: RequestCreateUser): ResponseCreateUser? {
        return withContext(Dispatchers.IO) {
            httpClient.post(
                urlString = "/v1/users",
                block =  {
                    setBody(data)
                    println(data)
                }
            ).getResponse<ResponseCreateUser>().success?.data
        }
    }
}