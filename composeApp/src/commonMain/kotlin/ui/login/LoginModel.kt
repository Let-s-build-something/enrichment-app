package ui.login

import androidx.lifecycle.viewModelScope
import base.navigation.NavigationNode
import base.utils.Matrix
import base.utils.Matrix.ErrorCode.USER_IN_USE
import base.utils.Matrix.LOGIN_EMAIL_IDENTITY
import base.utils.deeplinkHost
import base.utils.openLink
import coil3.toUri
import data.io.app.ClientStatus
import data.io.app.SecureSettingsKeys
import data.io.app.SettingsKeys.KEY_CLIENT_STATUS
import data.io.base.RecaptchaParams
import data.io.matrix.auth.AuthenticationCredentials
import data.io.matrix.auth.AuthenticationData
import data.io.matrix.auth.MatrixAuthenticationPlan
import data.io.matrix.auth.MatrixAuthenticationResponse
import data.io.matrix.auth.MatrixIdentifierData
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import utils.SharedLogger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun loginModule() = module {
    factory { LoginRepository(get()) }
    viewModelOf(::LoginModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class LoginModel(
    private val dataManager: LoginDataManager,
    private val repository: LoginRepository
): SharedModel() {
    data class HomeServerResponse(
        val state: HomeServerState,
        val plan: MatrixAuthenticationPlan? = null,
        val address: String,
        val supportsEmail: Boolean = false,
        val registrationEnabled: Boolean = true
    ) {
        override fun toString(): String {
            return "{" +
                    "state: $state, " +
                    "plan: $plan," +
                    "address: $address," +
                    "supportsEmail: $supportsEmail" +
                    "}"
        }
    }
    enum class HomeServerState {
        Valid,
        Invalid
    }

    private val _loginResult = MutableSharedFlow<LoginResultType?>()
    private val _isLoading = MutableStateFlow(false)
    private val _matrixAuthResponse = MutableStateFlow<MatrixAuthenticationResponse?>(null)
    private var session: String? = null

    /** current client status */
    val clientStatus = MutableStateFlow(ClientStatus.NEW)

    /** Sends signal to UI about a response that happened */
    val loginResult = _loginResult.asSharedFlow()
    val isLoading = _isLoading.asStateFlow()
    val homeServerResponse = dataManager.homeServerResponse.asStateFlow()
    val supportsEmail = dataManager.homeServerResponse.transform { res ->
        emit(
            res?.plan?.flows?.any {
                it.stages?.contains(LOGIN_EMAIL_IDENTITY) == true
            } != false
        )
    }

    private var ssoNonce: String?
        get() = runBlocking { settings.getStringOrNull(SecureSettingsKeys.KEY_LOGIN_NONCE) }
        set(value) {
            runBlocking {
                if(value == null) settings.remove(SecureSettingsKeys.KEY_LOGIN_NONCE)
                else settings.putString(SecureSettingsKeys.KEY_LOGIN_NONCE, value)
            }
        }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ClientStatus.entries.find {
                it.name == settings.getStringOrNull(KEY_CLIENT_STATUS)
            }?.let { status ->
                clientStatus.value = status
            }
        }
    }

    /** Changes loading status */
    fun setLoading(state: Boolean) {
        _isLoading.value = state
    }

    /** Clears current Matrix progress and cached information */
    fun clearMatrixProgress() {
        _matrixProgress.value = null
        ssoNonce = null
    }

    /** Validates Matrix username availability */
    fun validateUsername(
        address: String,
        username: String
    ) {
        if(username.isEmpty()) return
        viewModelScope.launch {
            repository.validateUsername(
                address = address,
                username = username
            )?.let {
                if(it.code == USER_IN_USE) {
                    _loginResult.emit(LoginResultType.USERNAME_EXISTS)
                }
            }
        }
    }

    private val homeserverAbilityCache = hashMapOf<String, Boolean>()

    /** Clears home server information */
    fun selectHomeServer(
        screenType: LoginScreenType,
        address: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            dataManager.homeServerResponse.value = (if(screenType == LoginScreenType.SIGN_UP) {
                repository.dummyMatrixRegister(address = address)
            }else repository.dummyMatrixLogin(address = address)).let { response ->
                if (response != null) {
                    session = response.session ?: session
                    HomeServerResponse(
                        state = if(response.flows != null) HomeServerState.Valid else HomeServerState.Invalid,
                        plan = response,
                        address = address,
                        supportsEmail = if(screenType == LoginScreenType.SIGN_UP) false else {
                            (homeserverAbilityCache[address] ?: (authService.loginWithIdentifier(
                                setupAutoLogin = false,
                                homeserver = address,
                                identifier = MatrixIdentifierData(
                                    type = Matrix.Id.THIRD_PARTY,
                                    medium = Matrix.Medium.EMAIL,
                                    address = "email@email.com"
                                ),
                                password = "-",
                                token = null
                            ).error?.message?.contains("Bad login type") == false)).also { answer ->
                                homeserverAbilityCache[address] = answer
                            }
                        }
                    )
                } else {
                    HomeServerResponse(
                        state = HomeServerState.Invalid,
                        address = address,
                        registrationEnabled = response?.error?.contains("Registration has been disabled.") == false
                    )
                }
            }
            _isLoading.value = false
        }
    }

    private val _matrixProgress = MutableStateFlow<MatrixProgress?>(null)
    val matrixProgress = _matrixProgress.asStateFlow()

    /** Makes a new request for a token on Matrix homeserver */
    fun matrixRequestToken() {
        if(_matrixProgress.value?.secret == null) return
        viewModelScope.launch {
            repository.requestRegistrationToken(
                secret = _matrixProgress.value?.secret ?: "",
                email = _matrixProgress.value?.email ?: "",
                address = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER
            ).also { res ->
                res.error?.retryAfterMs?.let {
                    _matrixProgress.value = _matrixProgress.value?.copy(retryAfter = it)
                }
                res.success?.data?.sid?.let {
                    _matrixProgress.value = _matrixProgress.value?.copy(sid = it)
                }
            }
        }
    }

    /** Submits recaptcha result and continues in the flow stages */
    fun matrixStepOver(
        type: String,
        recaptchaJson: String? = null,
        agreements: List<String>? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val recaptcha = if(recaptchaJson != null) {
                KoinPlatform.getKoin().get<Json>().decodeFromString<RecaptchaParams>(
                    recaptchaJson
                ).token
            }else _matrixProgress.value?.recaptcha
            val userAccepts = agreements ?: _matrixProgress.value?.agreements
            val lastIndex = matrixProgress.value?.response?.flows?.firstOrNull()?.stages?.lastIndex ?: 0

            _matrixProgress.value = _matrixProgress.value?.copy(
                recaptcha = recaptcha,
                agreements = userAccepts,
                index = _matrixProgress.value?.index?.plus(1)?.coerceAtMost(lastIndex) ?: 0
            )?.also { progress ->
                repository.registerWithUsername(
                    address = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                    username = progress.username ?: "",
                    password = progress.password,
                    authenticationData = AuthenticationData(
                        session = session,
                        type = type,
                        response = recaptcha,
                        userAccepts = userAccepts,
                        credentials = AuthenticationCredentials(
                            clientSecret = progress.secret ?: "",
                            sid = progress.sid ?: ""
                        )
                    )
                )?.also { response ->
                    if(progress.index == lastIndex) {
                        signUpWithPassword(
                            username = progress.username,
                            email = progress.email ?: "",
                            password = progress.password,
                            screenType = LoginScreenType.SIGN_UP,
                            response = response
                        )
                    }
                }
            }
        }
    }

    /** Requests signup with an email and a password */
    @OptIn(ExperimentalUuidApi::class)
    fun signUpWithPassword(
        username: String?,
        email: String,
        password: String,
        screenType: LoginScreenType,
        response: MatrixAuthenticationResponse? = null
    ) {
        SharedLogger.logger.debug { "signUpWithPassword, username: $username, email: $email, screenType: $screenType" }
        _isLoading.value = true
        viewModelScope.launch {
            val res = if(screenType == LoginScreenType.SIGN_UP && response == null) {
                val secret = Uuid.random().toString()

                // we check for single EP registration first
                val flow = dataManager.homeServerResponse.value?.plan?.flows?.firstOrNull()
                if(flow?.stages?.size == 1 && flow.stages.firstOrNull() == Matrix.LOGIN_DUMMY) {
                    repository.registerWithUsername(
                        address = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                        username = username,
                        password = password,
                        authenticationData = AuthenticationData(
                            session = session,
                            type = Matrix.LOGIN_DUMMY
                        )
                    )?.also {
                        session = it.session
                    }
                }else {
                    val requiresEmail = flow?.stages?.contains(LOGIN_EMAIL_IDENTITY) == true
                    // if flow contains email verification, we should check for duplicity first
                    val check = if(requiresEmail) {
                        repository.requestRegistrationToken(
                            secret = secret,
                            email = email,
                            address = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER
                        )
                    }else null

                    when(check?.error?.code) {
                        Matrix.ErrorCode.CREDENTIALS_IN_USE -> {
                            _isLoading.value = false
                            _loginResult.emit(LoginResultType.EMAIL_EXISTS)
                        }
                        Matrix.ErrorCode.CREDENTIALS_DENIED, Matrix.ErrorCode.FORBIDDEN, Matrix.ErrorCode.UNKNOWN -> {
                            _isLoading.value = false
                            _loginResult.emit(LoginResultType.FAILURE)
                        }
                        else -> {
                            (repository.registerWithUsername(
                                address = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                                username = null,
                                password = null,
                                authenticationData = AuthenticationData(
                                    session = null,
                                    type = null
                                )
                            ) ?: dataManager.homeServerResponse.value?.plan)?.also { response ->
                                session = response.session
                                _matrixProgress.value = MatrixProgress(
                                    username = username,
                                    email = email,
                                    password = password,
                                    response = response,
                                    secret = secret
                                )
                            }
                        }
                    }
                    return@launch
                }
            }else null ?: response
            _matrixAuthResponse.value = res

            if(screenType == LoginScreenType.SIGN_UP) {
                if(res?.userId != null || username == null) {
                    _matrixAuthResponse.value = res
                    clearMatrixProgress()

                    loginWithCredentials(
                        username = username,
                        email = email,
                        password = password
                    )
                }else {
                    _isLoading.value = false
                    _loginResult.emit(LoginResultType.FAILURE)
                }
            }else loginWithCredentials(username, email, password)
        }
    }

    /** Requests a sign-in with an email and a password */
    private suspend fun loginWithCredentials(
        username: String?,
        email: String,
        password: String
    ) {
        val res = if(_matrixAuthResponse.value?.userId == null) {
            val isUser = !username.isNullOrBlank()
            authService.loginWithIdentifier(
                homeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                identifier = MatrixIdentifierData(
                    type = if(isUser) Matrix.Id.USER else Matrix.Id.THIRD_PARTY,
                    medium = Matrix.Medium.EMAIL.takeIf { !isUser },
                    address = email.takeIf { !isUser },
                    user = username.takeIf { isUser }
                ),
                password = password,
                token = null
            ).let {
                if(it.error?.code == Matrix.ErrorCode.FORBIDDEN) {
                    _loginResult.emit(LoginResultType.INVALID_CREDENTIAL)
                    return
                }else if(it.error != null) {
                    _loginResult.emit(LoginResultType.FAILURE)
                    return
                }
                it.success?.data
            }
        }else _matrixAuthResponse.value

        if(res?.userId != null || username == null) {
            _matrixAuthResponse.value = res
            clearMatrixProgress()
            initUserObject()
        }else _loginResult.emit(LoginResultType.FAILURE)
    }

    /** Requests the URL for redirecting user */
    @OptIn(ExperimentalUuidApi::class)
    fun requestSsoRedirect(idpId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val ssoNonce = Uuid.random().toString()
            val homeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER
            this@LoginModel.ssoNonce = "${ssoNonce}_$homeserver"
            val redirectUrl = "${deeplinkHost}${NavigationNode.Login(nonce = ssoNonce).deepLink}"
            openLink(
                "https://${homeserver}/_matrix/client/v3/login/sso/redirect" +
                        (if(idpId != null)"/$idpId" else "") +
                        "?redirectUrl=${redirectUrl.toUri()}"
            )
        }
    }

    fun loginWithToken(nonce: String, token: String) {
        _isLoading.value = true
        val savedNonce = ssoNonce?.split("_")
        viewModelScope.launch {
            if(nonce != savedNonce?.firstOrNull()) {
                _loginResult.emit(LoginResultType.AUTH_SECURITY)
            }else {
                if(homeServerResponse.value == null) {
                    dataManager.homeServerResponse.value = HomeServerResponse(
                        address = savedNonce.lastOrNull() ?: "",
                        state = HomeServerState.Valid
                    )
                }

                authService.loginWithIdentifier(
                    homeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                    identifier = null,
                    password = null,
                    token = token
                ).let {
                    when {
                        it.success != null -> initUserObject()
                        it.error?.code == Matrix.ErrorCode.FORBIDDEN -> _loginResult.emit(LoginResultType.INVALID_CREDENTIAL)
                        else -> _loginResult.emit(LoginResultType.FAILURE)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    /** Authenticates user with a token */
    private suspend fun initUserObject() {
        _matrixAuthResponse.value?.accessToken?.let { accessToken ->
            updateClientSettings()
            withContext(Dispatchers.IO) {
                settings.putString(KEY_CLIENT_STATUS, ClientStatus.REGISTERED.name)
                SharedLogger.logger.debug { "User initialized as: ${sharedDataManager.currentUser.value}" }
            }
            _loginResult.emit(
                if (currentUser.value != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
            )
        }
    }
}

data class MatrixProgress(
    val username: String?,
    val email: String?,
    val secret: String?,
    val password: String,
    val recaptcha: String? = null,
    val sid: String? = null,
    val agreements: List<String>? = null,
    val response: MatrixAuthenticationPlan?,
    val index: Int = 0,
    val retryAfter: Int? = null
)
