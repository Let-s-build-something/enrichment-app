package lets.build.chatenrichment.ui.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squadris.squadris.compose.base.BaseViewModel
import com.squadris.squadris.utils.RefreshableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lets.build.chatenrichment.data.shared.SharedDataManager
import javax.inject.Inject

enum class LoginErrorType {
    NO_GOOGLE_CREDENTIALS
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sharedDataManager: SharedDataManager
): BaseViewModel() {

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    private val _loginErrorResponse = MutableSharedFlow<LoginErrorType?>()

    /** Sends signal to UI about an error that happened */
    val loginErrorResponse = _loginErrorResponse.asSharedFlow()

    /** Requests signup with an email and a password */
    fun signUpWithPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            Firebase.auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sharedDataManager.currentUser.value = Firebase.auth.currentUser
                        Log.d("kostka_test", "createUserWithEmail:success, user: ${sharedDataManager.currentUser.value}")
                    } else {
                        Log.w("kostka_test", "createUserWithEmail:failure", task.exception)
                    }
                }
        }
    }

    /**
     * Makes a request for google sign in with the user of credential manager
     * @param filterAuthorizedAccounts whether to filter authorized accounts
     */
    fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean = true,
        credentialManager: CredentialManager,
        context: Context,
        webClientId: String
    ) {
        Log.d("kostka_test", "requestGoogleSignIn")
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption
            .Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            //.setAutoSelectEnabled(true)
            .build()

        val passwordCredential = GetPasswordOption()

        val request: GetCredentialRequest = GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(passwordCredential)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                handleGoogleSignIn(result)
            } catch (e: NoCredentialException) {
                Log.e("kostka_test", "$e")
                if(filterAuthorizedAccounts) {
                    requestGoogleSignIn(
                        filterAuthorizedAccounts = false,
                        credentialManager = credentialManager,
                        context = context,
                        webClientId = webClientId
                    )
                }else {
                    _loginErrorResponse.emit(LoginErrorType.NO_GOOGLE_CREDENTIALS)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.e("kostka_test", "$e")
                /*if(filterAuthorizedAccounts) {
                    requestGoogleSignIn(
                        filterAuthorizedAccounts = false,
                        credentialManager = credentialManager,
                        context = context,
                        webClientId = webClientId
                    )
                }*/
            } catch (e: GetCredentialException) {
                Log.e("kostka_test", "$e")
            }
        }
    }

    /** Handles successful user sign in with any type of credential */
    private fun handleGoogleSignIn(result: GetCredentialResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            // Handle the successfully returned credential.
            val credential = result.credential

            Log.d("kostka_test", "Received credential: $credential")
            when (credential) {
                is PublicKeyCredential -> {
                    // Share responseJson such as a GetCredentialResponse on your server to
                    // validate and authenticate
                    val responseJson = credential.authenticationResponseJson
                }
                is PasswordCredential -> {
                    // Send ID and password to your server to validate and authenticate.
                    val id = credential.id
                    val password = credential.password

                    when {
                        android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches() -> {
                            Firebase.auth.signInWithEmailAndPassword(id, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        sharedDataManager.currentUser.value = Firebase.auth.currentUser
                                        Log.d("kostka_test", "signInWithEmail:success, user: ${sharedDataManager.currentUser.value}")
                                    } else {
                                        Log.w("kostka_test", "signInWithEmail:failure", task.exception)
                                    }
                                }
                        }
                        android.util.Patterns.PHONE.matcher(id).matches() -> {
                            val authCredential = PhoneAuthProvider.getCredential(id, password)

                            Firebase.auth.signInWithCredential(authCredential)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success
                                        sharedDataManager.currentUser.value = Firebase.auth.currentUser
                                        Log.d("kostka_test", "signInWithPhone:success, user: ${sharedDataManager.currentUser.value}")
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w("kostka_test", "signInWithPhone:failure", task.exception)
                                    }
                                }
                        }
                    }
                }
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            // Use googleIdTokenCredential and extract id to validate and
                            // authenticate on your server.
                            val googleIdTokenCredential = GoogleIdTokenCredential
                                .createFrom(credential.data)
                            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential
                                .idToken,
                                null
                            )
                            Firebase.auth.signInWithCredential(authCredential).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    sharedDataManager.currentUser.value = Firebase.auth.currentUser
                                    Log.d("kostka_test", "signInWithGoogle:success, user: ${sharedDataManager.currentUser.value}")
                                } else {
                                    Log.w("kostka_test", "signInWithGoogle:failure", task.exception)
                                }
                            }


                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e(
                                "kostka_test",
                                "Received an invalid google id token response: $e"
                            )
                        }
                    } else {
                        // Catch any unrecognized custom credential type here.
                        Log.e("kostka_test", "Unexpected type of credential")
                    }
                }
                else -> {
                    // Catch any unrecognized credential type here.
                    Log.e("kostka_test", "Unexpected type of credential")
                }
            }
        }
    }
}