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
import database.factory.SecretByteArray
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import koin.InterceptingEngine
import koin.SecureAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val authModule = module {
    single { AuthService() }
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
        const val TOKEN_REFRESH_THRESHOLD_MS = 2_000L
    }

    val awaitingAutologin: Boolean
        get() = secureSettings.hasKey(SecureSettingsKeys.KEY_CREDENTIALS)

    private val matrixClientFactory by lazy {
        MatrixClientFactory(
            getLoginInfo = {
                (retrieveCredentials()?.let { credentials ->
                    if (credentials.accessToken != null
                        && credentials.refreshToken != null
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
                    } else Result.failure(Throwable())
                } ?: Result.failure(Throwable()))
            },
            httpClientEngine = InterceptingEngine(
                engine = KoinPlatform.getKoin().get(),
                authService = this@AuthService,
                dataManager = dataManager
            ),
            saveDatabasePassword = { userId, key ->
                saveDatabasePassword(userId = userId, key = key)
            }
        )
    }

    fun clear() {
        stop()
        secureSettings.remove(SecureSettingsKeys.KEY_CREDENTIALS)
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
                println("kostka_test, expires in: ${(credentials.expiresAtMsEpoch ?: 0) - DateUtils.now.toEpochMilliseconds()}")

                when {
                    (!forceRefresh && credentials.isFullyValid)
                            || dataManager.networkConnectivity.value?.isNetworkAvailable == false -> {
                        updateUser(credentials = credentials)
                        if(!credentials.isExpired) {
                            println("kostka_test, setupAutoLogin -> not expired. Initializing matrix.")
                            initializeMatrixClient(auth = credentials)
                        }else {
                            println("kostka_test, setupAutoLogin -> expired, expect refresh soon.")
                        }
                        enqueueRefreshToken(
                            refreshToken = credentials.refreshToken,
                            expiresAtMsEpoch = credentials.expiresAtMsEpoch,
                            homeserver = credentials.homeserver
                        )
                    }
                    // something's missing, we gotta get all the info first
                    else -> {
                        println("kostka_test, setupAutoLogin -> login. AccessToken: ${credentials.accessToken}," +
                                " idToken: ${credentials.idToken}")
                        if(loginWithCredentials(forceRefresh = false)) setupAutoLogin(forceRefresh = false)
                    }
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

    fun storedUserId(): String? = secureSettings.getStringOrNull(
        key = SecureSettingsKeys.KEY_USER_ID
    )

    private suspend fun retrieveCredentials(): AuthItem? {
        return withContext(Dispatchers.IO) {
            secureSettings.getString(
                key = SecureSettingsKeys.KEY_CREDENTIALS,
                ""
            ).takeIf { it.isNotBlank() }?.let { res ->
                val decoded = json.decodeFromString<AuthItem>(res)
                val userId = storedUserId()
                decoded.copy(
                    deviceId = getDeviceId(userId),
                    pickleKey = getPickleKey(userId),
                    databasePassword = getDatabasePassword(userId),
                    userId = userId
                )
            }
        }
    }

    private suspend fun updateUser(credentials: AuthItem) = withContext(Dispatchers.Default) {
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
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun cacheCredentials(
        homeserver: String? = null,
        password: String? = null,
        identifier: MatrixIdentifierData? = null,
        deviceId: String? = null,
        response: MatrixAuthenticationResponse? = null
    ) {
        val user = dataManager.currentUser.value

        withContext(Dispatchers.IO) {
            val previous = retrieveCredentials()
            val userId = response?.userId ?: identifier?.user ?: previous?.userId
            val server = homeserver ?: response?.homeserver ?: previous?.homeserver
            val accessToken = response?.accessToken ?: previous?.accessToken
            println("kostka_test, cacheCredentials, new accessToken: $accessToken, previous: ${previous?.accessToken}")

            val credentials = AuthItem(
                accessToken = accessToken,
                refreshToken = response?.refreshToken ?: previous?.refreshToken,
                expiresAtMsEpoch = DateUtils.now.toEpochMilliseconds().plus(
                    response?.expiresInMs
                        ?: previous?.expiresAtMsEpoch?.minus(DateUtils.now.toEpochMilliseconds())
                        ?: DEFAULT_TOKEN_LIFESPAN_MS
                ).minus(TOKEN_REFRESH_THRESHOLD_MS),
                password = password ?: previous?.password,
                homeserver = server,
                userId = userId,
                loginType = identifier?.type ?: previous?.loginType,
                medium = identifier?.medium ?: previous?.medium,
                address = identifier?.address ?: previous?.address,
                deviceId = response?.deviceId ?: previous?.deviceId ?: deviceId ?: getDeviceId(userId),
                pickleKey = previous?.pickleKey ?: getPickleKey(userId) ?: Uuid.random().toString(),
                displayName = user?.displayName ?: previous?.displayName,
                tag = user?.tag ?: previous?.tag,
                publicId = user?.publicId ?: previous?.publicId,
                configuration = user?.configuration ?: previous?.configuration,
                idToken = user?.idToken ?: previous?.idToken,
                databasePassword = previous?.databasePassword
            )

            updateUser(credentials = credentials)
            secureSettings.putString(
                key = SecureSettingsKeys.KEY_CREDENTIALS,
                json.encodeToString(credentials)
            )
            if(credentials.userId != null) {
                secureSettings.putString(
                    key = SecureSettingsKeys.KEY_USER_ID,
                    value = credentials.userId
                )
            }
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
                    if(response is BaseResponse.Success) {
                        cacheCredentials(
                            response = response.data,
                            homeserver = homeserver,
                            password = null,
                            identifier = null,
                            deviceId = null
                        )
                        initializeMatrixClient()

                        refreshToken(
                            refreshToken = response.data.refreshToken,
                            expiresAtMsEpoch = DateUtils.now.toEpochMilliseconds()
                                .plus(response.data.expiresInMs ?: DEFAULT_TOKEN_LIFESPAN_MS)
                                .minus(TOKEN_REFRESH_THRESHOLD_MS),
                            homeserver = homeserver
                        )
                    }else loginWithCredentials(forceRefresh = true)
                }
            }
        }else loginWithCredentials(forceRefresh = true)
    }

    private suspend fun loginWithCredentials(forceRefresh: Boolean): Boolean {
        retrieveCredentials()?.let {
            val deviceId = it.deviceId ?: generateDeviceId()

            return when {
                !forceRefresh && !it.isExpired && it.accessToken != null && it.idToken == null -> {
                    authFirebase(
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken,
                        expiresInMs = null,
                        deviceId = deviceId
                    )
                    val isValid = dataManager.currentUser.value?.idToken != null
                    cacheCredentials(
                        deviceId = deviceId,
                        response = if(isValid) null else MatrixAuthenticationResponse(expiresInMs = 0)
                    )
                    loginWithCredentials(forceRefresh)
                }
                // only refresh
                it.isFullyValid && dataManager.currentUser.value?.isFullyValid == true -> {
                    refreshToken(
                        refreshToken = it.refreshToken,
                        expiresAtMsEpoch = it.expiresAtMsEpoch,
                        homeserver = it.homeserver
                    )
                    dataManager.currentUser.value?.isFullyValid == true
                }
                else -> {
                    loginWithIdentifier(
                        homeserver = it.homeserver ?: "",
                        identifier = MatrixIdentifierData(
                            type = it.loginType,
                            medium = it.medium,
                            address = it.address,
                            user = it.userId
                        ),
                        password = it.password,
                        deviceId = deviceId
                    ).success?.data != null
                }
            }
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
                    authFirebase(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        expiresInMs = response.expiresInMs,
                        deviceId = deviceId
                    )

                    cacheCredentials(
                        response = response,
                        identifier = identifier,
                        homeserver = homeserver,
                        password = password,
                        deviceId = deviceId
                    )
                    initializeMatrixClient()

                    coroutineScope {
                        if(isRunning) stop()
                        if(setupAutoLogin) setupAutoLogin()
                    }
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

    suspend fun createLoginRequest(): AuthenticationRequest? = withContext(Dispatchers.IO) {
        retrieveCredentials()?.let {
            if(it.password != null) {
                when(it.loginType) {
                    "m.id.user" -> IdentifierType.User(user = it.userId ?: "")
                    "m.id.thirdparty" -> IdentifierType.Thirdparty(
                        medium = it.medium ?: "",
                        address = it.address ?: ""
                    )
                    else -> null
                }?.let { identifier ->
                    AuthenticationRequest.Password(
                        identifier = identifier,
                        password = it.password
                    )
                }
            }else null
        }
    }

    private suspend fun authFirebase(
        accessToken: String?,
        refreshToken: String?,
        expiresInMs: Long?,
        deviceId: String?
    ) = withContext(Dispatchers.IO) {
        with(dataManager.currentUser) {
            // the init-app would fail due to missing idToken and accessToken
            if(value?.idToken == null || value?.accessToken == null) {
                val update = UserIO(
                    idToken = Firebase.auth.currentUser?.getIdToken(false),
                    accessToken = accessToken
                )
                value = value?.update(update) ?: update
            }
            if(value?.tag == null) {
                val update = repository.authenticateUser(
                    refreshToken = refreshToken,
                    expiresInMs = expiresInMs,
                    localSettings = dataManager.localSettings.value?.copy(
                        deviceId = deviceId
                    )
                )
                value = value?.update(update) ?: update
            }
        }
    }

    private suspend fun initializeMatrixClient(auth: AuthItem? = null) {
        val credentials = auth ?: retrieveCredentials() ?: return

        if(dataManager.matrixClient.value == null) {
            dataManager.matrixClient.value = matrixClientFactory.initializeMatrixClient(
                credentials = credentials
            )
        }
    }

    private fun saveDatabasePassword(
        userId: String? = null,
        key: SecretByteArray?
    ) {
        (userId ?: dataManager.currentUser.value?.matrixUserId)?.let { id ->
            when (key) {
                is SecretByteArray.AesHmacSha2 -> {
                    secureSettings.putString(
                        key = "${SecureSettingsKeys.KEY_DB_PASSWORD}_${id}",
                        value = json.encodeToString(key)
                    )
                }
                else -> {}
            }
        }
    }

    private fun getDatabasePassword(userId: String? = null): SecretByteArray? {
        return (userId ?: dataManager.currentUser.value?.matrixUserId)?.let { id ->
            secureSettings.getString(
                "${SecureSettingsKeys.KEY_DB_PASSWORD}_${id}", ""
            ).takeIf { it.isNotBlank() }?.let {
                json.decodeFromString<SecretByteArray.AesHmacSha2>(it)
            }
        }
    }

    //mac: BkLukuyyfn9OQ2xoQ3c82orML8G0sVdj95NCN04o9b0

    @OptIn(ExperimentalUuidApi::class)
    private fun generateDeviceId(): String = "${currentPlatform}_${Uuid.random().toHexString()}"
}
