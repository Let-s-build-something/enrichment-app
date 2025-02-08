package data.shared.auth

import augmy.interactive.shared.ui.base.currentPlatform
import base.utils.Matrix
import data.io.base.BaseResponse
import data.io.matrix.auth.EmailLoginRequest
import data.io.matrix.auth.MatrixAuthenticationResponse
import data.io.matrix.auth.MatrixIdentifierData
import data.shared.SharedDataManager
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.login.safeRequest

internal val authModule = module {
    factory { AuthService() }
}

class AuthService {
    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false


    // TODO get accessToken + email + password + username from settings

    fun autologin() {

    }

    private fun retrieveCredentials() {

    }

    private fun cacheCredentials() {

    }

    private fun enqueueRefreshToken() {

    }

    private fun refreshToken() {

    }

    /** Matrix login via email and username */
    suspend fun loginWithIdentifier(
        homeserver: String,
        identifier: MatrixIdentifierData,
        password: String?,
    ): BaseResponse<MatrixAuthenticationResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<MatrixAuthenticationResponse> {
                httpClient.post(url = Url("https://${homeserver}/_matrix/client/v3/login")) {
                    setBody(
                        EmailLoginRequest(
                            identifier = identifier,
                            initialDeviceDisplayName = "augmy.interactive.com: $currentPlatform",
                            password = password,
                            type = Matrix.LOGIN_PASSWORD
                        )
                    )
                }
            }
        }
    }
}
