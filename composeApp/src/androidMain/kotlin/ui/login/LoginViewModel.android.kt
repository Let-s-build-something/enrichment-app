package ui.login

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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import data.io.identity_platform.IdentityRefreshToken
import data.io.identity_platform.IdentityUserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

/** module providing platform-specific sign in options */
actual fun signInServiceModule() = module {
    val context: Context = getKoin().get()

    single<UserOperationServiceTEST> { UserOperationServiceTEST(context) }
}

actual class UserOperationServiceTEST(
    private val context: Context
) {

    actual val availableOptions = listOf(SingInServiceOption.GOOGLE)

    actual suspend fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean,
        webClientId: String
    ): LoginResultType {
        val pendingResult = checkForPendingResult()
        if(pendingResult != null) return pendingResult

        var result: LoginResultType? = null

        val credentialManager = CredentialManager.create(context)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption
            .Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val passwordCredential = GetPasswordOption()

        val request: GetCredentialRequest = GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(passwordCredential)
            .build()

        try {
            val res = credentialManager.getCredential(
                request = request,
                context = context,
            )
            result = handleGoogleSignIn(res)
        } catch (e: NoCredentialException) {
            Log.e("kostka_test", "$e")
            result = requestGoogleSignIn(
                filterAuthorizedAccounts = false,
                webClientId = webClientId
            )
        } catch (e: GetCredentialCancellationException) {
            Log.e("kostka_test", "$e")
            if(filterAuthorizedAccounts) {
                result = requestGoogleSignIn(
                    filterAuthorizedAccounts = false,
                    webClientId = webClientId
                )
            }
        } catch (e: GetCredentialException) {
            Log.e("kostka_test", "${e.errorMessage}")
        }

        return result ?: LoginResultType.FAILURE
    }

    actual suspend fun requestAppleSignIn(webClientId: String): LoginResultType {
        return LoginResultType.FAILURE
    }

    actual suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? = null
    actual suspend fun signInWithPassword(email: String, password: String): IdentityUserResponse? = null
    actual suspend fun refreshToken(refreshToken: String): IdentityRefreshToken? = null
}

/**
 * Checks for any currently ongoing process and only returns [LoginResultType] in case the pending exists,
 * thus, it should be uninterrupted
 */
private suspend fun checkForPendingResult(): LoginResultType? {
    val pending = Firebase.auth.pendingAuthResult

    return if (pending != null) {
        if(pending.await().user != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
    } else null
}

/** Handles successful user sign in with any type of credential */
private suspend fun handleGoogleSignIn(result: GetCredentialResponse): LoginResultType {
    // Handle the successfully returned credential.
    val credential = result.credential

    Log.d("kostka_test", "Received credential: $credential")
    return withContext(Dispatchers.IO) {
        when (credential) {
            is PublicKeyCredential -> {
                // Share responseJson such as a GetCredentialResponse on your server to
                // validate and authenticate
                //val responseJson = credential.authenticationResponseJson
                LoginResultType.FAILURE
            }
            is PasswordCredential -> {
                // Send ID and password to your server to validate and authenticate.
                val id = credential.id
                val password = credential.password

                val request = when {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches() -> {
                        Firebase.auth.signInWithEmailAndPassword(id, password).await()
                    }
                    android.util.Patterns.PHONE.matcher(id).matches() -> {
                        Firebase.auth.signInWithCredential(
                            PhoneAuthProvider.getCredential(id, password)
                        ).await()
                    }
                    else -> null
                }
                if(request?.user != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
            }
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val authCredential = GoogleAuthProvider.getCredential(
                            googleIdTokenCredential.idToken,
                            null
                        )
                        val request = Firebase.auth.signInWithCredential(authCredential).await()
                        if(request.user != null) LoginResultType.SUCCESS else LoginResultType.FAILURE
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(
                            "kostka_test",
                            "Received an invalid google id token response: $e"
                        )
                        LoginResultType.FAILURE
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e("kostka_test", "Unexpected type of credential")
                    LoginResultType.FAILURE
                }
            }
            else -> {
                // Catch any unrecognized credential type here.
                Log.e("kostka_test", "Unexpected type of credential")
                LoginResultType.FAILURE
            }
        }
    }
}