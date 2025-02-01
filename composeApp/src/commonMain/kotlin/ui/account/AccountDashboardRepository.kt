package ui.account

import augmy.interactive.shared.ui.base.currentPlatform
import base.utils.deviceName
import data.io.base.BaseResponse
import data.io.social.UserConfiguration
import data.shared.SharedRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

    @Serializable
    data class LogoutRequestBody(
        @SerialName("device_name")
        val deviceName: String
    )

    /** Logs user out of this device */
    suspend fun logout(): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<Any> {
                patch(
                    urlString = "/api/v1/users/logout",
                    block =  {
                        setBody(
                            LogoutRequestBody(deviceName = deviceName() ?: currentPlatform.name)
                        )
                    }
                )
            }
        }
    }
}
