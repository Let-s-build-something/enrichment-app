@file:OptIn(ExperimentalSettingsApi::class)

package ui.login

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.base.currentPlatform
import com.russhwolf.settings.ExperimentalSettingsApi
import data.io.app.ClientStatus
import data.io.app.SettingsKeys.KEY_CLIENT_STATUS
import data.io.identity_platform.IdentityMessageType
import data.io.user.RequestCreateUser
import data.io.user.UserIO
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthEmailException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.Module

/** Communication between the UI, the control layers, and control and data layers */
class LoginViewModel(
    private val serviceProvider: UserOperationService,
    private val repository: LoginRepository
): SharedViewModel() {

    private val _loginResult = MutableSharedFlow<LoginResultType?>()

    /** current client status */
    val clientStatus = MutableStateFlow(ClientStatus.NEW)

    /** Sends signal to UI about a response that happened */
    val loginResult = _loginResult.asSharedFlow()

    /** List of all available service via which user can sign in */
    val availableOptions = serviceProvider.availableOptions


    init {
        viewModelScope.launch(Dispatchers.IO) {
            ClientStatus.entries.find {
                it.name == settings.getStringOrNull(KEY_CLIENT_STATUS)
            }?.let { status ->
                clientStatus.value = status
            }
        }
    }

    /** Requests signup with an email and a password */
    fun signUpWithPassword(
        email: String,
        password: String,
        screenType: LoginScreenType
    ) {
        viewModelScope.launch {
            if(screenType == LoginScreenType.SIGN_UP) {
                serviceProvider.signUpWithPassword(email, password)?.let {
                    when(it) {
                        IdentityMessageType.SUCCESS -> {
                            signInWithPassword(email, password)
                        }
                        IdentityMessageType.EMAIL_EXISTS -> _loginResult.emit(LoginResultType.EMAIL_EXISTS)
                    }
                } ?: try {
                    Firebase.auth.createUserWithEmailAndPassword(email, password).user?.let {
                        signInWithPassword(email, password)
                    }
                } catch(e: FirebaseAuthUserCollisionException) {
                    _loginResult.emit(LoginResultType.EMAIL_EXISTS)
                }
            }else signInWithPassword(email, password)
        }
    }

    /** Requests a sign-in with an email and a password */
    private fun signInWithPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                Firebase.auth.signInWithEmailAndPassword(email, password)
                authenticateUser(email)
            }catch (e: FirebaseAuthInvalidCredentialsException) {
                _loginResult.emit(LoginResultType.INVALID_CREDENTIAL)
            }catch (e: FirebaseAuthEmailException) {
                _loginResult.emit(LoginResultType.INVALID_CREDENTIAL)
            }catch (e: Exception) {
                _loginResult.emit(LoginResultType.FAILURE)
            }
        }
    }

    /** Requests sign in or sign up via Google account */
    fun requestGoogleSignIn() {
        viewModelScope.launch {
            val res = serviceProvider.requestGoogleSignIn(filterAuthorizedAccounts = true)
            if(res == LoginResultType.SUCCESS) {
                finalizeSignIn(null)
            }else _loginResult.emit(res)
        }
    }

    /** Requests sign in or sign up via Google account */
    fun requestAppleSignIn() {
        viewModelScope.launch {
            val res = serviceProvider.requestAppleSignIn()
            if(res == LoginResultType.SUCCESS) {
                finalizeSignIn(null)
            }else _loginResult.emit(res)
        }
    }

    /** Authenticates user with a token */
    private suspend fun authenticateUser(email: String?) {
        withContext(Dispatchers.IO) {
            firebaseUser.firstOrNull()?.getIdToken(false)?.let { idToken ->
                sharedDataManager.currentUser.value = UserIO(idToken = idToken)
                sharedDataManager.currentUser.value = repository.authenticateUser(
                    localSettings = sharedDataManager.localSettings.value
                )?.copy(idToken = idToken)
            }
            finalizeSignIn(email)
        }
    }

    /** finalizes full flow with a result */
    private suspend fun finalizeSignIn(email: String?) {
        if(sharedDataManager.currentUser.value?.publicId == null) {
            Firebase.auth.currentUser?.uid?.let { clientId ->
                sharedDataManager.currentUser.value = UserIO(
                    publicId = repository.createUser(
                        RequestCreateUser(
                            email = email ?: try {
                                Firebase.auth.currentUser?.email
                            } catch (e: NotImplementedError) { null },
                            clientId = clientId,
                            platform = currentPlatform,
                            fcmToken = localSettings.value?.fcmToken
                        )
                    )?.publicId
                )
                authenticateUser(email)
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

/** available service via which user can sign in */
enum class SingInServiceOption {
    GOOGLE,
    APPLE
}

/** base URL for google cloud identity tool */
const val identityToolUrl = "https://identitytoolkit.googleapis.com/v1/accounts"

/** interface for communicating with all of the platforms creating sign in/up requests */
expect class UserOperationService {

    /** list of all available service via which user can sign in */
    val availableOptions: List<SingInServiceOption>

    /** Requests signup or sign in via Google account */
    suspend fun requestGoogleSignIn(filterAuthorizedAccounts: Boolean): LoginResultType

    /** Requests signup or sign in via Apple id */
    suspend fun requestAppleSignIn(): LoginResultType

    /** Requests a signup with email and password */
    suspend fun signUpWithPassword(email: String, password: String): IdentityMessageType?
}