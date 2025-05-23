package data.shared.auth

import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.utils.DateUtils
import base.utils.Matrix
import base.utils.Matrix.ErrorCode.UNKNOWN_TOKEN
import base.utils.deviceName
import base.utils.sha256
import data.io.app.LocalSettings
import data.io.app.SecureSettingsKeys
import data.io.base.AppPing
import data.io.base.AppPingType
import data.io.base.BaseResponse
import data.io.matrix.auth.EmailLoginRequest
import data.io.matrix.auth.MatrixAuthenticationResponse
import data.io.matrix.auth.MatrixIdentifierData
import data.io.matrix.auth.RefreshTokenRequest
import data.io.matrix.auth.local.AuthItem
import data.io.user.UserIO
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.sync.DataService
import data.shared.sync.DataSyncService
import database.factory.SecretByteArray
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import koin.InterceptingEngine
import koin.SecureAppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val logger = KotlinLogging.logger(name = "AuthServiceLogger")

    private val _httpClient by lazy { KoinPlatform.getKoin().inject<HttpClient>() }
    private val _dataManager by lazy { KoinPlatform.getKoin().inject<SharedDataManager>() }
    private val _dataService by lazy { KoinPlatform.getKoin().inject<DataService>() }
    private val _secureSettings by lazy { KoinPlatform.getKoin().inject<SecureAppSettings>() }
    private val _repository by lazy { KoinPlatform.getKoin().inject<SharedRepository>() }
    private val _json by lazy { KoinPlatform.getKoin().inject<Json>() }
    private val _syncService by lazy { KoinPlatform.getKoin().inject<DataSyncService>() }

    private val httpClient
        get() = _httpClient.value
    private val dataManager
        get() = _dataManager.value
    private val dataService
        get() = _dataService.value
    private val secureSettings
        get() = _secureSettings.value
    private val repository
        get() = _repository.value
    private val json
        get() = _json.value
    private val syncService
        get() = _syncService.value

    private val enqueueScope = CoroutineScope(Job())
    private val mutex = Mutex()
    private var isRunning = false

    companion object {
        private const val DEFAULT_TOKEN_LIFESPAN_MS = 30 * 60 * 1_000L
        private const val DELAY_REFRESH_TOKEN_START = 5_000L
        const val TOKEN_REFRESH_THRESHOLD_MS = 2_000L
    }

    suspend fun getDeviceId() = dataManager.localSettings.value?.deviceId ?: koin.secureSettings.getString(
        SecureSettingsKeys.KEY_DEVICE_ID, ""
    ).takeIf { it.isNotBlank() } ?: generateDeviceId()

    val awaitingAutologin: Boolean
        get() = secureSettings.hasKey(SecureSettingsKeys.KEY_CREDENTIALS)

    private val matrixClientFactory by lazy {
        MatrixClientFactory(
            getLoginInfo = {
                retrieveCredentials().also {
                    logger.debug { "getLoginInfo, isFullyValid: ${it?.isFullyValid}" }
                }?.let { credentials ->
                    val deviceId = dataManager.localSettings.value?.deviceId
                    if (credentials.accessToken != null
                        //&& credentials.refreshToken != null TODO some homeservers do not support refresh tokens
                        && credentials.userId != null
                        && deviceId != null
                    ) {
                        Result.success(
                            LoginInfo(
                                userId = UserId(credentials.userId),
                                deviceId = deviceId,
                                accessToken = credentials.accessToken,
                                refreshToken = credentials.refreshToken
                            )
                        )
                    } else Result.failure(Throwable())
                } ?: Result.failure(Throwable())
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
        logger.debug { "clear" }
        secureSettings.remove(SecureSettingsKeys.KEY_CREDENTIALS)
    }

    fun stop() {
        if(isRunning) {
            isRunning = false
            enqueueScope.coroutineContext.cancelChildren()
        }
    }

    suspend fun setupAutoLogin(forceRefresh: Boolean = false) {
        when {
            forceRefresh -> stop()
            mutex.isLocked -> return
            isRunning -> return
        }

        withContext(Dispatchers.IO) {
            retrieveCredentials().also {
                logger.debug { "setupAutoLogin, credentials: $it" }
            }?.let { credentials ->
                logger.debug { "expires in: ${(credentials.expiresAtMsEpoch ?: 0) - DateUtils.now.toEpochMilliseconds()}, " +
                        "isFullyValid: ${credentials.isFullyValid}, forceRefresh: $forceRefresh"
                }

                when {
                    (!forceRefresh && credentials.isFullyValid)
                            || dataManager.networkConnectivity.value?.isNetworkAvailable == false -> {
                        updateUser(credentials = credentials)
                        if(!credentials.isExpired) {
                            logger.debug { "setupAutoLogin -> not expired. Initializing matrix." }
                            initializeMatrixClient(auth = credentials)
                        }else {
                            if(credentials.refreshToken == null) {
                                //dataService.appendPing(AppPing(AppPingType.HardLogout))
                                logger.debug { "setupAutoLogin -> HardLogout" }
                            }else logger.debug { "setupAutoLogin -> expired, expect refresh soon." }
                        }
                        enqueueRefreshToken(
                            refreshToken = credentials.refreshToken,
                            expiresAtMsEpoch = credentials.expiresAtMsEpoch,
                            homeserver = credentials.homeserver
                        )
                    }
                    // something's missing, we gotta get all the info first
                    else -> {
                        println("setupAutoLogin -> login. AccessToken: ${credentials.accessToken}," +
                                " idToken: ${credentials.idToken}")
                        if(loginWithCredentials(forceRefresh = false)) setupAutoLogin(forceRefresh = false)
                    }
                }
            }
        }
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

        dataManager.localSettings.update {
            (it ?: LocalSettings()).copy(
                pickleKey = credentials.pickleKey ?: dataManager.localSettings.value?.pickleKey
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun cacheCredentials(
        homeserver: String? = null,
        password: String? = null,
        token: String?,
        identifier: MatrixIdentifierData? = null,
        response: MatrixAuthenticationResponse? = null
    ) {
        val user = dataManager.currentUser.value

        withContext(Dispatchers.IO) {
            val previous = retrieveCredentials()
            val userId = response?.userId ?: identifier?.user ?: previous?.userId
            val server = homeserver ?: response?.homeserver ?: previous?.homeserver
            val accessToken = response?.accessToken ?: previous?.accessToken

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
                pickleKey = previous?.pickleKey ?: getPickleKey(userId) ?: Uuid.random().toString(),
                displayName = user?.displayName ?: previous?.displayName,
                tag = user?.tag ?: previous?.tag,
                publicId = user?.publicId ?: previous?.publicId,
                configuration = user?.configuration ?: previous?.configuration,
                idToken = user?.idToken ?: previous?.idToken,
                databasePassword = previous?.databasePassword,
                token = token ?: previous?.token
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
            if(credentials.pickleKey != null) {
                secureSettings.putString(
                    key = "${SecureSettingsKeys.KEY_PICKLE_KEY}_${credentials.userId}",
                    value = credentials.pickleKey
                )
            }
            val newCredentials = retrieveCredentials()
            logger.debug { "credentials after caching: $newCredentials" }
        }
    }

    private fun enqueueRefreshToken(
        refreshToken: String?,
        homeserver: String?,
        expiresAtMsEpoch: Long?
    ) {
        if(!isRunning) {
            isRunning = true

            enqueueScope.launch {
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
        if(refreshToken != null && expiresAtMsEpoch != null && homeserver != null) {
            withContext(Dispatchers.IO) {
                val delay = expiresAtMsEpoch - DateUtils.now.toEpochMilliseconds()
                logger.debug { "refreshToken -> delay: $delay" }
                if(delay > 0) {
                    try { delay(delay) }catch (_: Exception) { }
                }
                logger.debug { "refreshToken after delay, refreshing" }
                httpClient.safeRequest<MatrixAuthenticationResponse> {
                    httpClient.post(urlString = "https://${homeserver}/_matrix/client/v3/refresh") {
                        setBody(
                            RefreshTokenRequest(refreshToken = refreshToken)
                        )
                    }
                }.let { response ->
                    when (response) {
                        is BaseResponse.Success -> {
                            cacheCredentials(
                                response = response.data,
                                homeserver = homeserver,
                                password = null,
                                identifier = null,
                                token = null
                            )
                            initializeMatrixClient()

                            logger.debug { "expires in: ${response.data.expiresInMs}" }
                            refreshToken(
                                refreshToken = response.data.refreshToken,
                                expiresAtMsEpoch = DateUtils.now.toEpochMilliseconds()
                                    .plus(response.data.expiresInMs ?: DEFAULT_TOKEN_LIFESPAN_MS)
                                    .minus(TOKEN_REFRESH_THRESHOLD_MS),
                                homeserver = homeserver
                            )
                        }
                        is BaseResponse.Error -> {
                            if(response.code == UNKNOWN_TOKEN) {
                                logger.debug { "Attempt to hard logout" }
                                dataService.appendPing(AppPing(AppPingType.HardLogout))
                            }else loginWithCredentials(false)
                        }
                        else -> {
                            delay(TOKEN_REFRESH_THRESHOLD_MS)
                            setupAutoLogin()
                        }
                    }
                }
            }
        }else loginWithCredentials(forceRefresh = false)
    }

    private suspend fun loginWithCredentials(forceRefresh: Boolean): Boolean {
        retrieveCredentials()?.let { credentials ->
            return when {
                !forceRefresh && !credentials.isExpired && credentials.accessToken != null && credentials.idToken == null -> {
                    authFirebase(
                        accessToken = credentials.accessToken,
                        refreshToken = credentials.refreshToken,
                        expiresInMs = null
                    )
                    val isValid = dataManager.currentUser.value?.idToken != null
                    if(isValid) {
                        cacheCredentials(
                            response = if(isValid) null else MatrixAuthenticationResponse(expiresInMs = 0),
                            token = null
                        )
                        true
                    }else false
                }
                // only refresh
                credentials.isExpired && credentials.refreshToken != null -> {
                    refreshToken(
                        refreshToken = credentials.refreshToken,
                        expiresAtMsEpoch = credentials.expiresAtMsEpoch,
                        homeserver = credentials.homeserver
                    )
                    logger.debug { "loginWithCredentials -> refresh -> isFullyValid: ${dataManager.currentUser.value?.isFullyValid}" }
                    dataManager.currentUser.value?.isFullyValid == true
                }
                credentials.canLogin && credentials.refreshToken == null -> {
                    logger.debug { "loginWithCredentials -> login" }
                    loginWithIdentifier(
                        homeserver = credentials.homeserver ?: "",
                        identifier = MatrixIdentifierData(
                            type = credentials.loginType,
                            medium = credentials.medium,
                            address = credentials.address,
                            user = credentials.userId
                        ),
                        password = credentials.password,
                        token = credentials.token
                    ).success?.data != null
                    false
                }
                else -> false
            }
        }
        return false
    }

    /** Matrix login via email and username */
    suspend fun loginWithIdentifier(
        setupAutoLogin: Boolean = true,
        homeserver: String,
        identifier: MatrixIdentifierData?,
        password: String?,
        token: String?,
    ): BaseResponse<MatrixAuthenticationResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<MatrixAuthenticationResponse> {
                httpClient.post(url = Url("https://${homeserver}/_matrix/client/v3/login")) {
                    setBody(
                        EmailLoginRequest(
                            identifier = identifier,
                            initialDeviceDisplayName = deviceName() ?: currentPlatform.name,
                            password = password,
                            type = if(token != null) Matrix.LOGIN_TOKEN else Matrix.LOGIN_PASSWORD,
                            deviceId = getDeviceId(),
                            token = token
                        )
                    )
                }
            }.also {
                it.success?.data?.let { response ->
                    authFirebase(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        expiresInMs = response.expiresInMs
                    )

                    cacheCredentials(
                        response = response,
                        identifier = identifier ?: MatrixIdentifierData(
                            address = "${it.success?.data?.userId?.replace("@", "")?.replace(":", "@")}"
                        ),
                        homeserver = homeserver,
                        password = password ?: sha256(it.success?.data?.userId),
                        token = token
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
                    localSettings = dataManager.localSettings.value
                )
                value = value?.update(update) ?: update
            }
        }
    }

    private suspend fun initializeMatrixClient(auth: AuthItem? = null) {
        val credentials = auth ?: retrieveCredentials() ?: return

        if(dataManager.matrixClient.value == null) {
            dataManager.matrixClient.value = matrixClientFactory.initializeMatrixClient(
                credentials = credentials,
                deviceId = getDeviceId()
            ).also {
                logger.debug { "new Matrix client: $it" }
                // attempt to start up the app
                syncService.sync(homeserver = dataManager.currentUser.value?.matrixHomeserver ?: "")
            }
        }else syncService.sync(homeserver = dataManager.currentUser.value?.matrixHomeserver ?: "")
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
                        value = json.encodeToString(key).also {
                            logger.debug { "saveDatabasePassword: $it, id: $id" }
                        }
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

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun generateDeviceId(): String = withContext(Dispatchers.Default) {
        "${currentPlatform}_${Uuid.random().toHexString()}".also {
            secureSettings.putString(SecureSettingsKeys.KEY_DEVICE_ID, it)
            dataManager.localSettings.update { prev ->
                prev?.copy(deviceId = it) ?: LocalSettings(deviceId = it)
            }
        }
    }
}
