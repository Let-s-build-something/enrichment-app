package ui.login

import androidx.lifecycle.viewModelScope
import data.io.identity_platform.IdentityMessageType
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.module.Module

/** Type of result that can be received by the sign in services */
enum class LoginResultType {

    /** general error, the request failed */
    FAILURE,

    /** the UI is missing window, iOS specific */
    NO_WINDOW,

    /** There are no credentials on the device, Android specific */
    NO_GOOGLE_CREDENTIALS,

    /** successful request, user is signed in */
    SUCCESS
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
    suspend fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean,
        webClientId: String
    ): LoginResultType

    /** Requests signup or sign in via Apple id */
    suspend fun requestAppleSignIn(): LoginResultType

    /** Requests a signup with email and password */
    suspend fun signUpWithPassword(email: String, password: String): IdentityMessageType?
}

/** Communication between the UI, the control layers, and control and data layers */
class LoginViewModel(
    private val serviceProvider: UserOperationService
): SharedViewModel() {

    private val _loginResult = MutableSharedFlow<LoginResultType?>()

    /** Sends signal to UI about a response that happened */
    val loginResult = _loginResult.asSharedFlow()

    /** List of all available service via which user can sign in */
    val availableOptions = serviceProvider.availableOptions

    /** Requests signup with an email and a password */
    fun signUpWithPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            serviceProvider.signUpWithPassword(email, password)?.let {
                when(it) {
                    IdentityMessageType.SUCCESS -> {
                        // TODO createUser our BE
                        signInWithPassword(email, password)
                    }
                    IdentityMessageType.EMAIL_EXISTS -> signInWithPassword(email, password)
                }
            } ?: try {
                // TODO createUser our BE
                processAuthResult(
                    Firebase.auth.createUserWithEmailAndPassword(email, password)
                )
            } catch(e: FirebaseAuthUserCollisionException) {
                println("FirebaseAuthUserCollisionException: The email address is already in use by another account.")
                signInWithPassword(email, password)
            }
        }
    }

    /** Requests a sign-in with an email and a password */
    private fun signInWithPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            processAuthResult(
                Firebase.auth.signInWithEmailAndPassword(email, password)
            )
        }
    }

    /** Requests sign in or sign up via Google account */
    fun requestGoogleSignIn(webClientId: String) {
        viewModelScope.launch {
            _loginResult.emit(
                serviceProvider.requestGoogleSignIn(
                    filterAuthorizedAccounts = true,
                    webClientId = webClientId
                )
            )
        }
    }

    /** Requests sign in or sign up via Google account */
    fun requestAppleSignIn() {
        viewModelScope.launch {
            _loginResult.emit(
                serviceProvider.requestAppleSignIn()
            )
        }
    }

    /** processes a given user if there is any */
    private suspend fun processAuthResult(user: AuthResult?) {
        _loginResult.emit(
            if (user != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
        )
    }
}