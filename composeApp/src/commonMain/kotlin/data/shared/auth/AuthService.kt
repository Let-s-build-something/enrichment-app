package data.shared.auth

import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.utils.DateUtils
import base.utils.Matrix
import data.io.app.LocalSettings
import data.io.app.SecureSettingsKeys
import data.io.base.BaseResponse
import data.io.matrix.auth.EmailLoginRequest
import data.io.matrix.auth.MatrixAuthenticationResponse
import data.io.matrix.auth.MatrixIdentifierData
import data.io.matrix.auth.RefreshTokenRequest
import data.io.matrix.auth.local.AuthItem
import data.io.user.UserIO
import data.shared.SharedDataManager
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import koin.SecureAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val authModule = module {
    factory { AuthService() }
}

/**
 * Service handling and caching final authorization.
 * The DAO informs us of all users on the device, whereas secure settings stores the credentials of the last login.
 */
class AuthService {
    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val secureSettings: SecureAppSettings by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()

    private val mutex = Mutex()
    private var isRunning = false

    companion object {
        private const val DEFAULT_TOKEN_LIFESPAN_MS = 30 * 60 * 1000L
    }

    val awaitingAutologin: Boolean
        get() = secureSettings.hasKey(SecureSettingsKeys.KEY_CREDENTIALS)

    fun clear() {
        stop()
        secureSettings.remove(SecureSettingsKeys.KEY_CREDENTIALS)
    }

    fun stop() {
        isRunning = false
        if(mutex.isLocked) mutex.unlock()
    }

    suspend fun setupAutoLogin() {
        if(isRunning) return

        withContext(Dispatchers.IO) {
            retrieveCredentials()?.let { credentials ->
                // we either use existing token or enqueue its refresh which results either in success or new login
                if((credentials.expiresAtMsEpoch ?: 0) > DateUtils.now.toEpochMilliseconds()
                    && credentials.accessToken != null
                ) {
                    updateUser(credentials = credentials)
                }else {
                    enqueueRefreshToken(
                        refreshToken = credentials.refreshToken,
                        expiresAtMsEpoch = credentials.expiresAtMsEpoch,
                        homeserver = credentials.homeserver
                    )
                }
            }
        }
    }

    private suspend fun retrieveCredentials(): AuthItem? {
        return withContext(Dispatchers.IO) {
            secureSettings.getString(
                key = SecureSettingsKeys.KEY_CREDENTIALS,
                ""
            ).takeIf { it.isNotBlank() }?.let {
                json.decodeFromString<AuthItem>(it)
            }
        }
    }

    private fun updateUser(credentials: AuthItem) {
        if(sharedDataManager.currentUser.value != null) {
            sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.copy(
                accessToken = credentials.accessToken ?: sharedDataManager.currentUser.value?.accessToken,
                matrixHomeserver = credentials.homeserver ?: sharedDataManager.currentUser.value?.matrixHomeserver,
                matrixUserId = credentials.userId ?: sharedDataManager.currentUser.value?.matrixUserId
            )
        }else {
            sharedDataManager.currentUser.value = UserIO(
                accessToken = credentials.accessToken,
                matrixHomeserver = credentials.homeserver,
                matrixUserId = credentials.userId
            )
        }
        val update = LocalSettings(
            deviceId = credentials.deviceId ?: sharedDataManager.localSettings.value?.deviceId,
            pickleKey = credentials.pickleKey ?: sharedDataManager.localSettings.value?.pickleKey
        )
        sharedDataManager.localSettings.value = sharedDataManager.localSettings.value?.update(update) ?: update
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun cacheCredentials(
        homeserver: String?,
        password: String?,
        identifier: MatrixIdentifierData?,
        response: MatrixAuthenticationResponse
    ) {
        withContext(Dispatchers.IO) {
            val previous = retrieveCredentials()
            val userId = response.userId ?: identifier?.user ?: previous?.userId
            val server = homeserver ?: response.homeserver ?: previous?.homeserver
            val accessToken = response.accessToken ?: previous?.accessToken

            val credentials = AuthItem(
                accessToken = accessToken,
                refreshToken = response.refreshToken ?: previous?.refreshToken,
                expiresAtMsEpoch = DateUtils.now.toEpochMilliseconds()
                    .plus(response.expiresInMs ?: DEFAULT_TOKEN_LIFESPAN_MS),
                password = password ?: previous?.password,
                homeserver = server,
                userId = userId,
                loginType = identifier?.type ?: previous?.loginType,
                medium = identifier?.medium ?: previous?.medium,
                address = identifier?.address ?: previous?.address,
                pickleKey = previous?.pickleKey ?: Uuid.random().toString(),
                deviceId = previous?.deviceId ?: currentPlatform.name.plus("_${Uuid.random()}")
            )

            updateUser(credentials = credentials)
            secureSettings.putString(
                key = SecureSettingsKeys.KEY_CREDENTIALS,
                json.encodeToString(credentials)
            )
        }
    }

    private suspend fun enqueueRefreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        if(!isRunning) {
            isRunning = true
            refreshToken(
                refreshToken = refreshToken,
                homeserver = homeserver,
                expiresAtMsEpoch = expiresAtMsEpoch
            )
        }
    }

    private suspend fun refreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        mutex.withLock {
            if(refreshToken != null && expiresAtMsEpoch != null && homeserver != null) {
                withContext(Dispatchers.IO) {
                    val delay = DateUtils.now.toEpochMilliseconds() - expiresAtMsEpoch
                    if(delay > 0) delay(delay)
                    httpClient.safeRequest<MatrixAuthenticationResponse> {
                        httpClient.post(urlString = "https://${homeserver}/_matrix/client/v3/refresh") {
                            setBody(
                                RefreshTokenRequest(refreshToken = refreshToken)
                            )
                        }
                    }.let { response ->
                        if(response is BaseResponse.Success && isRunning) {
                            cacheCredentials(
                                response = response.data,
                                password = null,
                                identifier = null,
                                homeserver = homeserver
                            )
                            enqueueRefreshToken(
                                refreshToken = response.data.refreshToken,
                                expiresAtMsEpoch = DateUtils.now.toEpochMilliseconds()
                                    .plus(response.data.expiresInMs ?: DEFAULT_TOKEN_LIFESPAN_MS),
                                homeserver = homeserver
                            )
                        }else loginWithCredentials()
                    }
                }
            }else loginWithCredentials()
        }
    }

    private suspend fun loginWithCredentials() {
        retrieveCredentials()?.let {
            loginWithIdentifier(
                homeserver = it.homeserver ?: "",
                identifier = MatrixIdentifierData(
                    type = it.loginType,
                    medium = it.medium,
                    address = it.address,
                    user = it.userId
                ),
                password = it.password
            )
        }
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
            }.also {
                it.success?.data?.let { response ->
                    cacheCredentials(
                        response = response,
                        identifier = identifier,
                        homeserver = homeserver,
                        password = password
                    )
                    enqueueRefreshToken(
                        refreshToken = response.refreshToken,
                        expiresAtMsEpoch = DateUtils.now.toEpochMilliseconds()
                            .plus(response.expiresInMs ?: DEFAULT_TOKEN_LIFESPAN_MS),
                        homeserver = homeserver
                    )
                }
                if(sharedDataManager.networkConnectivity.value?.isNetworkAvailable == false) {
                    retrieveCredentials()?.let { credentials ->
                        if(sharedDataManager.currentUser.value?.matrixUserId == null) {
                            updateUser(credentials = credentials)
                        }
                        delay(5000)
                        enqueueRefreshToken(
                            refreshToken = credentials.refreshToken,
                            expiresAtMsEpoch = credentials.expiresAtMsEpoch,
                            homeserver = credentials.homeserver
                        )
                    }
                }
            }
        }
    }
}
