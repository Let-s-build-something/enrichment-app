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
import data.shared.SharedRepository
import data.shared.sync.DataSyncService.Companion.SYNC_INTERVAL
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import koin.SecureAppSettings
import koin.httpClientConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.client.MatrixClientConfiguration.CacheExpireDurations
import net.folivo.trixnity.client.MatrixClientConfiguration.SyncLoopDelays
import net.folivo.trixnity.client.createTrixnityDefaultModuleFactories
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.login.safeRequest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
    private val dataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val secureSettings: SecureAppSettings by KoinPlatform.getKoin().inject()
    private val repository: SharedRepository by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()

    private val mutex = Mutex()
    private var isRunning = false

    companion object {
        private const val DEFAULT_TOKEN_LIFESPAN_MS = 30 * 60 * 1_000L
        private const val DELAY_REFRESH_TOKEN_START = 5_000L
    }

    val awaitingAutologin: Boolean
        get() = secureSettings.hasKey(SecureSettingsKeys.KEY_CREDENTIALS)

    fun clear() {
        stop()
        secureSettings.remove(SecureSettingsKeys.KEY_CREDENTIALS)
        secureSettings.clear()
    }

    fun stop() {
        if(isRunning) {
            isRunning = false
            if(mutex.isLocked) mutex.unlock()
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
                val currentTime = DateUtils.now.toEpochMilliseconds()
                println("kostka_test, expires in: ${(credentials.expiresAtMsEpoch ?: 0) - currentTime}")

                // we either use existing token or enqueue its refresh which results either in success or new login
                when {
                    !credentials.isFullyValid || forceRefresh -> {
                        println("kostka_test, invalid setupAutoLogin, accessToken: ${credentials.accessToken}," +
                                " idToken: ${credentials.idToken}")
                        if(loginWithCredentials()) setupAutoLogin(forceRefresh = false)
                    }
                    (credentials.expiresAtMsEpoch?.plus(2_000) ?: 0) > currentTime
                            && credentials.accessToken != null
                            && !forceRefresh -> {
                        updateUser(credentials = credentials)
                        enqueueRefreshToken(
                            refreshToken = credentials.refreshToken,
                            expiresAtMsEpoch = currentTime,
                            homeserver = credentials.homeserver
                        )
                    }
                    else -> enqueueRefreshToken(
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

    private suspend fun updateUser(credentials: AuthItem) {
        val userUpdate = UserIO(
            accessToken = credentials.accessToken ?: dataManager.currentUser.value?.accessToken,
            matrixHomeserver = credentials.homeserver ?: dataManager.currentUser.value?.matrixHomeserver,
            matrixUserId = credentials.userId ?: dataManager.currentUser.value?.matrixUserId,
            publicId = credentials.publicId ?: dataManager.currentUser.value?.publicId,
            configuration = credentials.configuration ?: dataManager.currentUser.value?.configuration,
            tag = credentials.tag ?: dataManager.currentUser.value?.tag,
            displayName = credentials.displayName ?: dataManager.currentUser.value?.displayName,
            idToken = credentials.idToken ?: dataManager.currentUser.value?.idToken
        )

        dataManager.currentUser.value = dataManager.currentUser.value?.update(userUpdate) ?: userUpdate

        val settingsUpdate = LocalSettings(
            deviceId = credentials.deviceId ?: dataManager.localSettings.value?.deviceId,
            pickleKey = credentials.pickleKey ?: dataManager.localSettings.value?.pickleKey
        )
        dataManager.localSettings.value = dataManager.localSettings.value?.update(settingsUpdate) ?: settingsUpdate

        initializeMatrixClient(credentials = credentials)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun cacheCredentials(
        homeserver: String?,
        password: String?,
        identifier: MatrixIdentifierData?,
        deviceId: String?,
        user: UserIO?,
        response: MatrixAuthenticationResponse
    ) {
        withContext(Dispatchers.IO) {
            val previous = retrieveCredentials()
            val userId = response.userId ?: identifier?.user ?: previous?.userId
            val server = homeserver ?: response.homeserver ?: previous?.homeserver
            val accessToken = response.accessToken ?: previous?.accessToken
            println("kostka_test, cacheCredentials, new accessToken: $accessToken, previous: ${previous?.accessToken}")

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
                pickleKey = previous?.pickleKey ?: getPickleKey(userId) ?: Uuid.random().toString(),
                displayName = user?.displayName ?: previous?.displayName,
                tag = user?.tag ?: previous?.tag,
                publicId = user?.publicId ?: previous?.publicId,
                configuration = user?.configuration ?: previous?.configuration,
                idToken = user?.idToken ?: previous?.idToken
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

            initializeMatrixClient(credentials = credentials)
        }
    }

    private suspend fun enqueueRefreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        if(!isRunning) {
            isRunning = true

            mutex.withLock {
                refreshToken(
                    refreshToken = refreshToken,
                    homeserver = homeserver,
                    expiresAtMsEpoch = expiresAtMsEpoch
                )
            }
        }
    }

    private suspend fun refreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        if(refreshToken != null && expiresAtMsEpoch != null && homeserver != null) {
            withContext(Dispatchers.IO) {
                val delay = expiresAtMsEpoch - DateUtils.now.toEpochMilliseconds()
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
                            homeserver = homeserver,
                            deviceId = null,
                            user = null
                        )
                        refreshToken(
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

    private suspend fun loginWithCredentials(): Boolean {
        retrieveCredentials()?.let {
            return loginWithIdentifier(
                homeserver = it.homeserver ?: "",
                identifier = MatrixIdentifierData(
                    type = it.loginType,
                    medium = it.medium,
                    address = it.address,
                    user = it.userId
                ),
                password = it.password,
                deviceId = it.deviceId ?: generateDeviceId()
            ).success?.data != null
        }
        return false
    }

    /** Matrix login via email and username */
    suspend fun loginWithIdentifier(
        setupAutoLogin: Boolean = true,
        homeserver: String,
        identifier: MatrixIdentifierData,
        password: String?,
        deviceId: String = generateDeviceId()
    ): BaseResponse<MatrixAuthenticationResponse> {
        // if this is a point of entry

        println("kostka_test, login with deviceId: $deviceId")
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<MatrixAuthenticationResponse> {
                httpClient.post(url = Url("https://${homeserver}/_matrix/client/v3/login")) {
                    setBody(
                        EmailLoginRequest(
                            identifier = identifier,
                            initialDeviceDisplayName = deviceName() ?: deviceId,
                            password = password,
                            type = Matrix.LOGIN_PASSWORD,
                            deviceId = deviceId
                        )
                    )
                }
            }.also {
                it.success?.data?.let { response ->
                    var user = dataManager.currentUser.value

                    if(dataManager.currentUser.value?.tag == null) {
                        // the init-app would fail due to missing idToken and accessToken
                        if(user?.idToken == null || user.accessToken == null) {
                            dataManager.currentUser.update { prev ->
                                val update = UserIO(
                                    idToken = Firebase.auth.currentUser?.getIdToken(false),
                                    accessToken = response.accessToken
                                )
                                prev?.update(update) ?: update
                            }
                        }

                        user = repository.authenticateUser(
                            refreshToken = response.refreshToken,
                            expiresInMs = response.expiresInMs,
                            localSettings = dataManager.localSettings.value?.copy(
                                deviceId = deviceId
                            )
                        )
                        println("kostka_test, new user: $user")
                        dataManager.currentUser.value = dataManager.currentUser.value?.update(user) ?: user
                    }

                    cacheCredentials(
                        response = response,
                        identifier = identifier,
                        homeserver = homeserver,
                        password = password,
                        deviceId = deviceId,
                        user = dataManager.currentUser.value
                    )
                    if(setupAutoLogin) setupAutoLogin()
                }

                if(setupAutoLogin && dataManager.networkConnectivity.value?.isNetworkAvailable == false) {
                    retrieveCredentials()?.let { credentials ->
                        if(dataManager.currentUser.value?.matrixUserId == null) {
                            updateUser(credentials = credentials)
                        }
                        delay(DELAY_REFRESH_TOKEN_START)
                        setupAutoLogin()
                    }
                }
            }
        }
    }

    private suspend fun initializeMatrixClient(credentials: AuthItem) {
        if(dataManager.matrixClient.value == null) {
            dataManager.matrixClient.value = MatrixClient.loginWith(
                baseUrl = Url("https://${credentials.homeserver}"),
                getLoginInfo = {
                    retrieveCredentials()?.let { credentials ->
                        if(credentials.accessToken != null
                            && credentials.refreshToken != null
                            && credentials.expiresAtMsEpoch != null
                            && credentials.userId != null
                            && credentials.deviceId != null
                        ) {
                            Result.success(
                                LoginInfo(
                                    userId = UserId(credentials.userId),
                                    deviceId = credentials.deviceId,
                                    accessToken = credentials.accessToken,
                                    refreshToken = credentials.refreshToken
                                )
                            )
                        }else Result.failure(Throwable())
                    } ?: Result.failure(Throwable())
                },
                repositoriesModuleFactory = {
                    createInMemoryRepositoriesModule()
                },
                mediaStoreFactory = {
                    InMemoryMediaStore()
                }
            ) {
                name = credentials.deviceId
                syncLoopDelays = SyncLoopDelays(
                    syncLoopDelay = 0L.seconds,
                    syncLoopErrorDelay = 10.seconds
                )
                lastRelevantEventFilter = { roomEvent ->
                    roomEvent is RoomEvent.MessageEvent<*>
                }
                storeTimelineEventContentUnencrypted = false
                modulesFactories = createTrixnityDefaultModuleFactories()
                httpClientEngine = KoinPlatform.getKoin().get()
                cacheExpireDurations = CacheExpireDurations.default(30.minutes)
                syncLoopTimeout = SYNC_INTERVAL.milliseconds
                httpClientConfig = {
                    httpClientConfig(sharedModel = KoinPlatform.getKoin().get())
                }
            }.getOrNull()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateDeviceId(): String = "${currentPlatform}_${Uuid.random().toHexString()}"
}
