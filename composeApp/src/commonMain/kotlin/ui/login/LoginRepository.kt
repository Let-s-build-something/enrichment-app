package ui.login

import augmy.interactive.shared.ui.base.currentPlatform
import base.utils.Matrix
import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.matrix.auth.AuthenticationData
import data.io.matrix.auth.EmailLoginRequest
import data.io.matrix.auth.EmailRegistrationRequest
import data.io.matrix.auth.MatrixAuthenticationPlan
import data.io.matrix.auth.MatrixAuthenticationResponse
import data.io.matrix.auth.MatrixIdentifierData
import data.io.matrix.auth.MatrixRegistrationRequest
import data.io.matrix.auth.MatrixTokenRequest
import data.io.matrix.auth.MatrixTokenResponse
import data.io.matrix.auth.UsernameValidationResponse
import data.io.user.RequestCreateUser
import data.io.user.ResponseCreateUser
import data.shared.SharedRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class LoginRepository(private val httpClient: HttpClient): SharedRepository(httpClient) {

    /** Makes a request to create a user */
    suspend fun createUser(data: RequestCreateUser): ResponseCreateUser? {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ResponseCreateUser> {
                post(
                    urlString = "/api/v1/users",
                    block =  {
                        setBody(data)
                    }
                )
            }.success?.data
        }
    }

    /** Retrieves the request token for further registration */
    suspend fun requestRegistrationToken(
        address: String,
        email: String,
        secret: String?,
        attempt: Int? = 1
    ): BaseResponse<MatrixTokenResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<MatrixTokenResponse> {
                httpClient.post(url = Url("https://${address}/_matrix/client/v3/register/email/requestToken")) {
                    setBody(
                        MatrixTokenRequest(
                            email = email,
                            clientSecret = secret,
                            sendAttempt = attempt
                        )
                    )
                }
            }
        }
    }

    /** Matrix registration via email and username */
    suspend fun registerWithUsername(
        address: String?,
        username: String?,
        password: String?,
        authenticationData: AuthenticationData
    ): MatrixAuthenticationResponse? {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequestError<MatrixAuthenticationResponse> {
                post(url = Url("https://${address}/_matrix/client/v3/register?kind=user")) {
                    setBody(
                        EmailRegistrationRequest(
                            auth = authenticationData,
                            password = password,
                            username = username,
                            initialDeviceDisplayName = "augmy.interactive.com: $currentPlatform"
                        )
                    )
                }
            }
        }
    }

    /** Retrieves options for registering with the given Matrix homeserver address */
    suspend fun dummyMatrixRegister(address: String): MatrixAuthenticationPlan? {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequestError<MatrixAuthenticationPlan> {
                post(url = Url("https://${address}/_matrix/client/v3/register")) {
                    setBody(
                        MatrixRegistrationRequest(
                            initialDeviceDisplayName = "augmy.interactive.com: $currentPlatform"
                        )
                    )
                }
            }
        }
    }

    /** Matrix login via email and username */
    suspend fun loginWithUsername(
        address: String,
        identifier: MatrixIdentifierData,
        password: String?,
    ): BaseResponse<MatrixAuthenticationResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<MatrixAuthenticationResponse> {
                httpClient.post(url = Url("https://${address}/_matrix/client/v3/login")) {
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

    /** Retrieves options for login with the given Matrix homeserver address */
    suspend fun dummyMatrixLogin(address: String): MatrixAuthenticationPlan? {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequestError<MatrixAuthenticationPlan> {
                get(url = Url("https://${address}/_matrix/client/v3/login"))
            }
        }
    }

    /** Retrieves options for login with the given Matrix homeserver address */
    suspend fun validateUsername(
        address: String,
        username: String
    ): UsernameValidationResponse? {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequestError<UsernameValidationResponse> {
                get(url = Url("https://${address}/_matrix/client/v3/register/available?username=$username"))
            }
        }
    }
}

suspend inline fun <reified T> HttpClient.safeRequest(
    block: HttpClient.() -> HttpResponse
): BaseResponse<T> = try {
    block().getResponse<T>()
} catch (e: UnresolvedAddressException) {
    e.printStackTrace()
    BaseResponse.Error()
}catch (e: Exception) {
    e.printStackTrace()
    BaseResponse.Error()
}

/** Parses both the success and error body into the [T] */
suspend inline fun <reified T> HttpClient.safeRequestError(
    block: HttpClient.() -> HttpResponse
): T? = try {
    block().body<T>()
}catch (e: Exception) {
    e.printStackTrace()
    null
}
