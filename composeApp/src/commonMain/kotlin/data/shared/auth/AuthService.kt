package data.shared.auth

import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.utils.DateUtils
import base.utils.Matrix
import base.utils.deviceName
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
import data.shared.crypto.OlmCryptoStore
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
    private var lockIdentifier = ""

    companion object {
        private const val DEFAULT_TOKEN_LIFESPAN_MS = 30 * 60 * 1000L
    }

    val awaitingAutologin: Boolean
        get() = secureSettings.hasKey(SecureSettingsKeys.KEY_CREDENTIALS)

    suspend fun clear() {
        stop()
        KoinPlatform.getKoin().getOrNull<OlmCryptoStore>()?.clear()
        secureSettings.remove(SecureSettingsKeys.KEY_CREDENTIALS)
        secureSettings.clear()
    }

    fun stop() {
        if(isRunning) {
            isRunning = false
            if(mutex.isLocked) mutex.unlock(owner = lockIdentifier)
        }
    }

    suspend fun setupAutoLogin(forceRefresh: Boolean = false) {
        when {
            forceRefresh -> stop()
            mutex.isLocked -> return
            isRunning -> return
        }

        withContext(Dispatchers.IO) {
            retrieveCredentials()?.let { credentials ->
                // we either use existing token or enqueue its refresh which results either in success or new login
                if((credentials.expiresAtMsEpoch ?: 0) > DateUtils.now.toEpochMilliseconds()
                    && credentials.accessToken != null
                    && !forceRefresh
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

    private suspend fun getDeviceId(userId: String?): String? = withContext(Dispatchers.IO) {
        if(userId == null) return@withContext null
        secureSettings.getString(
            "${SecureSettingsKeys.KEY_DEVICE_ID}_${userId}", ""
        ).takeIf { it.isNotBlank() }
    }

    private suspend fun getPickleKey(userId: String?): String? = withContext(Dispatchers.IO) {
        if(userId == null) return@withContext null
        secureSettings.getString(
            "${SecureSettingsKeys.KEY_PICKLE_KEY}_${userId}", ""
        ).takeIf { it.isNotBlank() }
    }

    private suspend fun retrieveCredentials(): AuthItem? {
        return withContext(Dispatchers.IO) {
            secureSettings.getString(
                key = SecureSettingsKeys.KEY_CREDENTIALS,
                ""
            ).takeIf { it.isNotBlank() }?.let { res ->
                val decoded = json.decodeFromString<AuthItem>(res)
                decoded.copy(
                    deviceId = getDeviceId(decoded.userId),
                    pickleKey = getPickleKey(decoded.userId)
                )
            }
        }
    }

    private fun updateUser(credentials: AuthItem) {
        sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.copy(
            accessToken = credentials.accessToken ?: sharedDataManager.currentUser.value?.accessToken,
            matrixHomeserver = credentials.homeserver ?: sharedDataManager.currentUser.value?.matrixHomeserver,
            matrixUserId = credentials.userId ?: sharedDataManager.currentUser.value?.matrixUserId
        ) ?: UserIO(
            accessToken = credentials.accessToken,
            matrixHomeserver = credentials.homeserver,
            matrixUserId = credentials.userId
        )

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
        deviceId: String?,
        response: MatrixAuthenticationResponse
    ) {
        withContext(Dispatchers.IO) {
            val previous = retrieveCredentials()
            println("kostka_test, cacheCredentials, deviceId: ${response.deviceId ?: deviceId ?: getDeviceId(response.userId)}")
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
                deviceId = response.deviceId ?: previous?.deviceId ?: deviceId ?: getDeviceId(userId),
                pickleKey = previous?.pickleKey ?: getPickleKey(userId) ?: Uuid.random().toString()
            )

            updateUser(credentials = credentials)
            secureSettings.putString(
                key = SecureSettingsKeys.KEY_CREDENTIALS,
                json.encodeToString(credentials)
            )
            if(credentials.deviceId != null) {
                secureSettings.putString(
                    key = "${SecureSettingsKeys.KEY_DEVICE_ID}_${credentials.userId}",
                    value = credentials.deviceId
                )
            }
            if(credentials.pickleKey != null) {
                secureSettings.putString(
                    key = "${SecureSettingsKeys.KEY_PICKLE_KEY}_${credentials.userId}",
                    value = credentials.pickleKey
                )
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun enqueueRefreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        if(!isRunning) {
            isRunning = true

            if(mutex.tryLock(owner = Uuid.random().toHexString().also {
                lockIdentifier = it
            })) {
                mutex.withLock {
                    refreshToken(
                        refreshToken = refreshToken,
                        homeserver = homeserver,
                        expiresAtMsEpoch = expiresAtMsEpoch
                    )
                }
            }
        }
    }

    private suspend fun refreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        println("kostka_test, refreshToken, homeserver: $homeserver, refreshToken: $refreshToken, expiresAtMsEpoch: $expiresAtMsEpoch")
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
                    println("kostka_test, is success: ${response is BaseResponse.Success}, isRunning: $isRunning")
                    if(response is BaseResponse.Success && isRunning) {
                        cacheCredentials(
                            response = response.data,
                            password = null,
                            identifier = null,
                            homeserver = homeserver,
                            deviceId = null
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
                password = it.password,
                deviceId = it.deviceId
            )
        }
    }

    /** Matrix login via email and username */
    suspend fun loginWithIdentifier(
        homeserver: String,
        identifier: MatrixIdentifierData,
        password: String?,
        deviceId: String? = generateDeviceId()
    ): BaseResponse<MatrixAuthenticationResponse> {
        // if this is a point of entry
        if(!isRunning) isRunning = true
        println("kostka_test, login with deviceId: $deviceId")
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<MatrixAuthenticationResponse> {
                httpClient.post(url = Url("https://${homeserver}/_matrix/client/v3/login")) {
                    setBody(
                        EmailLoginRequest(
                            identifier = identifier,
                            initialDeviceDisplayName = "$currentPlatform-${deviceName()}",
                            password = password,
                            type = Matrix.LOGIN_PASSWORD,
                            deviceId = deviceId
                        )
                    )
                }
            }.also {
                it.success?.data?.let { response ->
                    cacheCredentials(
                        response = response,
                        identifier = identifier,
                        homeserver = homeserver,
                        password = password,
                        deviceId = deviceId
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

    @OptIn(ExperimentalUuidApi::class)
    private fun generateDeviceId(): String = "${currentPlatform}_${Uuid.random().toHexString()}"
}
