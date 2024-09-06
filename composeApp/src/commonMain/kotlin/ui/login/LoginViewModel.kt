package ui.login

import androidx.lifecycle.viewModelScope
import data.io.CloudUserHelper
import data.io.identity_platform.IdentityRefreshToken
import data.io.identity_platform.IdentityUserResponse
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
    FAILURE,
    NO_GOOGLE_CREDENTIALS,
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
const val secureTokenUrl = "https://securetoken.googleapis.com"

/** interface for communicating with all of the platforms creating sign in/up requests */
expect class UserOperationServiceTEST {

    /** list of all available service via which user can sign in */
    val availableOptions: List<SingInServiceOption>

    /** Requests signup or sign in via Google account */
    suspend fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean,
        webClientId: String
    ): LoginResultType

    /** Requests signup or sign in via Apple id */
    suspend fun requestAppleSignIn(webClientId: String): LoginResultType

    /** Requests signup or sign in via Apple id */
    suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse?

    /** Requests signup or sign in via Apple id */
    suspend fun signInWithPassword(email: String, password: String): IdentityUserResponse?

    /** Requests for a new refreshToken and secures user sign in */
    suspend fun refreshToken(refreshToken: String): IdentityRefreshToken?
}

/** Communication between the UI, the control layers, and control and data layers */
class LoginViewModel(
    private val serviceProvider: UserOperationServiceTEST
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
                processIdentityResult(it)
            } ?: try {
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
    fun signInWithPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            serviceProvider.signInWithPassword(email, password)?.let {
                processIdentityResult(it)
            } ?: processAuthResult(
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
    fun requestAppleSignIn(webClientId: String) {
        viewModelScope.launch {
            _loginResult.emit(
                serviceProvider.requestAppleSignIn(webClientId = webClientId)
            )
        }
    }

    /** processes a given user if there is any */
    private suspend fun processAuthResult(user: AuthResult?) {
        _loginResult.emit(
            if (user != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
        )
    }

    /** processes a given user info if there is any */
    private suspend fun processIdentityResult(response: IdentityUserResponse?) {
        if(response?.email.isNullOrBlank().not()) {
            overrideCurrentUser(CloudUserHelper.fromUserResponse(response))
        }
        _loginResult.emit(
            if (response != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
        )
    }
}