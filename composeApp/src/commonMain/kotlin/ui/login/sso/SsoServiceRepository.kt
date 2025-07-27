package ui.login.sso

import base.utils.Matrix.Id.AUGMY_OIDC
import data.shared.auth.AuthService
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

class SsoServiceRepository(
    private val httpClient: HttpClient,
    private val authService: AuthService
) {

    suspend fun registerWithGoogle(
        homeserver: String,
        displayName: String?,
        idToken: String,
        issuer: String,
        nonce: String
    ) = withContext(Dispatchers.IO) {
        httpClient.safeRequest<AugmySsoResponse> {
            post("https://$homeserver/_synapse/client/sso/registration") {
                setBody(
                    AugmySsoRequest(
                        type = AUGMY_OIDC,
                        idToken = idToken,
                        displayName = displayName,
                        issuer = issuer,
                        nonce = nonce,
                        deviceId = authService.getDeviceId()
                    )
                )
            }
        }
    }

    suspend fun loginWithGoogle(
        homeserver: String,
        idToken: String,
        issuer: String,
        nonce: String
    ) = withContext(Dispatchers.IO) {
        httpClient.safeRequest<AugmySsoResponse> {
            post("https://$homeserver/_synapse/client/sso/login") {
                setBody(
                    AugmySsoRequest(
                        type = AUGMY_OIDC,
                        idToken = idToken,
                        issuer = issuer,
                        nonce = nonce,
                        deviceId = authService.getDeviceId()
                    )
                )
            }
        }
    }
}