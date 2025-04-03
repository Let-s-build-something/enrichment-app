package ui.login

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import base.navigation.NavigationNode
import base.utils.Matrix
import base.utils.Matrix.ErrorCode.USER_IN_USE
import base.utils.deeplinkHost
import base.utils.openLink
import base.utils.sha256
import coil3.toUri
import data.io.app.ClientStatus
import data.io.app.SettingsKeys.KEY_CLIENT_STATUS
import data.io.base.RecaptchaParams
import data.io.identity_platform.IdentityMessageType
import data.io.identity_platform.IdentityUserResponse
import data.io.matrix.auth.AuthenticationCredentials
import data.io.matrix.auth.AuthenticationData
import data.io.matrix.auth.MatrixAuthenticationPlan
import data.io.matrix.auth.MatrixAuthenticationResponse
import data.io.matrix.auth.MatrixIdentifierData
import data.io.user.RequestCreateUser
import data.io.user.UserIO
import data.shared.SharedModel
import data.shared.auth.AuthService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthEmailException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Communication between the UI, the control layers, and control and data layers */
class LoginModel(
    private val serviceProvider: UserOperationService,
    private val dataManager: LoginDataManager,
    private val repository: LoginRepository,
    private val authService: AuthService
): SharedModel() {
    data class HomeServerResponse(
        val state: HomeServerState,
        val plan: MatrixAuthenticationPlan? = null,
        val address: String,
        val supportsEmail: Boolean = false
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
            }else repository.dummyMatrixLogin(address = address))?.let {
                session = it.session ?: session
                HomeServerResponse(
                    state = if(it.flows != null) HomeServerState.Valid else HomeServerState.Invalid,
                    plan = it,
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
            } ?: HomeServerResponse(
                state = HomeServerState.Invalid,
                address = address
            )
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
                    val requiresEmail = flow?.stages?.contains(Matrix.LOGIN_EMAIL_IDENTITY) == true
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
                            _loginResult.emit(LoginResultType.EMAIL_EXISTS)
                        }
                        Matrix.ErrorCode.CREDENTIALS_DENIED, Matrix.ErrorCode.FORBIDDEN, Matrix.ErrorCode.UNKNOWN -> {
                            _loginResult.emit(LoginResultType.FAILURE)
                        }
                        else -> {
                            if(isEmailFreeFirebase(email = email, password = password)) {
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
                            }else _loginResult.emit(LoginResultType.EMAIL_EXISTS)
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

                    serviceProvider.signUpWithPassword(email, password)?.let { userResponse ->
                        when {
                            userResponse.idToken != null -> {
                                sharedDataManager.currentUser.update {
                                    it?.copy(idToken = userResponse.idToken) ?: UserIO(idToken = userResponse.idToken)
                                }
                                loginWithCredentials(
                                    username = username,
                                    email = email,
                                    password = password
                                )
                            }
                            userResponse.error?.type == IdentityMessageType.EMAIL_EXISTS -> _loginResult.emit(LoginResultType.EMAIL_EXISTS)
                            else -> _loginResult.emit(LoginResultType.FAILURE)
                        }
                    } ?: try {
                        Firebase.auth.createUserWithEmailAndPassword(email, password).user?.authenticateUser(email)
                    } catch(e: FirebaseAuthUserCollisionException) {
                        _loginResult.emit(LoginResultType.EMAIL_EXISTS)
                    } catch(e: Exception) {
                        _loginResult.emit(LoginResultType.FAILURE)
                    }
                }else _loginResult.emit(LoginResultType.FAILURE)
            }else loginWithCredentials(username, email, password)
        }
    }

    /** Hack method to find out whether email is available in Firebase */
    private suspend fun isEmailFreeFirebase(
        email: String?,
        password: String
    ): Boolean {
        return if(email == null) false
        else withContext(Dispatchers.IO) {
            try {
                serviceProvider.signUpWithPassword(email, password, deleteRightAfter = true)?.let {
                    it.idToken != null
                } ?: Firebase.auth.createUserWithEmailAndPassword(
                    email = email,
                    password = password
                ).user?.let {
                    it.delete()
                    true
                } ?: false
            }catch (e: FirebaseAuthUserCollisionException) {
                false
            }
        }
    }

    /** Requests a sign-in with an email and a password */
    private suspend fun loginWithCredentials(
        username: String?,
        email: String,
        password: String
    ) {
        val res = if(_matrixAuthResponse.value?.userId == null) {
            authService.loginWithIdentifier(
                setupAutoLogin = false,
                homeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                identifier = MatrixIdentifierData(
                    type = Matrix.Id.THIRD_PARTY,
                    medium = Matrix.Medium.EMAIL,
                    address = email,
                    user = username
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
            try {
                Firebase.auth.signInWithEmailAndPassword(email, password).user
                    ?.authenticateUser(email)
            }catch (e: FirebaseAuthInvalidCredentialsException) {
                _loginResult.emit(LoginResultType.INVALID_CREDENTIAL)
            }catch (e: FirebaseAuthEmailException) {
                _loginResult.emit(LoginResultType.INVALID_CREDENTIAL)
            }catch (e: Exception) {
                _loginResult.emit(LoginResultType.FAILURE)
            }
        }else _loginResult.emit(LoginResultType.FAILURE)
    }

    /** Requests the URL for redirecting user */
    @OptIn(ExperimentalUuidApi::class)
    fun requestSsoRedirect(idpId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            dataManager.ssoNonce = Uuid.random().toString()
            val ssoNonce = dataManager.ssoNonce
            val homeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER
            val redirectUrl = "${deeplinkHost}${NavigationNode.Login(nonce = ssoNonce).deepLink}"
            openLink("https://${homeserver}/_matrix/client/v3/login/sso/redirect/$idpId?redirectUrl=${redirectUrl.toUri()}")
        }
    }

    fun loginWithToken(nonce: String, token: String) {
        _isLoading.value = true
        viewModelScope.launch {
            if(nonce != dataManager.ssoNonce) {
                _loginResult.emit(LoginResultType.AUTH_SECURITY)
            }else {
                _loginResult.emit(
                    authService.loginWithIdentifier(
                        setupAutoLogin = true,
                        homeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER,
                        identifier = null,
                        password = null,
                        token = token
                    ).let {
                        when {
                            it.success != null -> {
                                val email = "${it.success?.data?.userId?.replace("@", "")?.replace(":", "@")}"
                                val password = sha256(it.success?.data?.userId)

                                try {
                                    Firebase.auth.createUserWithEmailAndPassword(email, password).user?.authenticateUser(email)
                                } catch(e: FirebaseAuthUserCollisionException) {
                                    Firebase.auth.signInWithEmailAndPassword(email, password).user?.authenticateUser(email)
                                } catch(e: Exception) {
                                    Firebase.auth.signInAnonymously().user?.authenticateUser(email)
                                }

                                LoginResultType.SUCCESS
                            }
                            it.error?.code == Matrix.ErrorCode.FORBIDDEN -> LoginResultType.INVALID_CREDENTIAL
                            else -> LoginResultType.FAILURE
                        }
                    }
                )
            }
            _isLoading.value = false
        }
    }

    /** Authenticates user with a token */
    private suspend fun FirebaseUser.authenticateUser(email: String?, attempt: Int = 1) {
        withContext(Dispatchers.IO) {
            (if(currentUser.value?.idToken == null) {
                this@authenticateUser.getIdToken(false)
            }else currentUser.value?.idToken)?.let { idToken ->
                if(sharedDataManager.currentUser.value?.idToken == null) {
                    val initialUser = UserIO(
                        idToken = idToken,
                        accessToken = _matrixAuthResponse.value?.accessToken,
                        matrixHomeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER
                    )
                    sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(initialUser) ?: initialUser
                }
                if(sharedDataManager.currentUser.value?.tag == null) {
                    sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(
                        repository.authenticateUser(
                            localSettings = sharedDataManager.localSettings.value,
                            refreshToken = _matrixAuthResponse.value?.refreshToken,
                            expiresInMs = _matrixAuthResponse.value?.expiresInMs
                        )
                    )
                }
                if(attempt < 3) {
                    this@authenticateUser.finalizeSignIn(email, attempt = attempt)
                }else _loginResult.emit(LoginResultType.FAILURE)
            }.ifNull { _loginResult.emit(LoginResultType.FAILURE) }
        }
    }

    /** finalizes full flow with a result */
    private suspend fun FirebaseUser?.finalizeSignIn(email: String?, attempt: Int = 1) {
        if(sharedDataManager.currentUser.value?.publicId == null) {
            Firebase.auth.currentUser?.uid?.let { clientId ->
                sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.copy(
                    publicId = repository.createUser(
                        RequestCreateUser(
                            email = email ?: try {
                                Firebase.auth.currentUser?.email
                            } catch (e: NotImplementedError) { null },
                            clientId = clientId,
                            fcmToken = localSettings.value?.fcmToken,
                            matrixUserId = _matrixAuthResponse.value?.userId,
                            matrixHomeserver = dataManager.homeServerResponse.value?.address ?: AUGMY_HOME_SERVER
                        )
                    )?.publicId
                )
                this?.authenticateUser(email, attempt + 1)
                clientId
            }.ifNull {
                _loginResult.emit(LoginResultType.FAILURE)
            }
        }else {
            (if(sharedDataManager.currentUser.value != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    settings.putString(KEY_CLIENT_STATUS, ClientStatus.REGISTERED.name)
                }
                LoginResultType.SUCCESS
            } else LoginResultType.FAILURE).also {
                _loginResult.emit(it)
            }
        }
    }
}

/** module providing platform-specific sign in options */
expect fun signInServiceModule(): Module

/** base URL for google cloud identity tool */
const val identityToolUrl = "https://identitytoolkit.googleapis.com/v1/accounts"

/** interface for communicating with all of the platforms creating sign in/up requests */
expect class UserOperationService {

    /** Requests signup or sign in via Google account */
    suspend fun requestGoogleSignIn(filterAuthorizedAccounts: Boolean): LoginResultType

    /** Requests signup or sign in via Apple id */
    suspend fun requestAppleSignIn(): LoginResultType

    /** Requests a signup with email and password */
    suspend fun signUpWithPassword(
        email: String,
        password: String,
        deleteRightAfter: Boolean = false
    ): IdentityUserResponse?
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
