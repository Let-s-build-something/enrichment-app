package ui.login.sso

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.firebase_web_client_id
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import data.io.base.BaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import ui.login.LoginResultType
import java.util.UUID

/** Module providing platform-specific sign-in options */
actual fun ssoServiceModule() = module {
    val context: Context = getKoin().get()
    factory { SsoServiceRepository(get(), get()) }
    single<SsoService> { SsoService(get(), context) }
}

actual class SsoService(
    private val repository: SsoServiceRepository,
    private val context: Context
) {
    companion object {
        private const val TAG = "UserOperationService"
    }

    private var lastNonce: String? = null

    actual suspend fun requestGoogleSignIn(
        homeserver: String,
        filterAuthorizedAccounts: Boolean
    ): BaseResponse<AugmySsoResponse> {
        val nonce = UUID.randomUUID().toString()
        lastNonce = nonce

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .setServerClientId(getString(Res.string.firebase_web_client_id))
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context as Activity
            )
            return handleSignIn(
                homeserver = homeserver,
                result,
                nonce,
                "https://accounts.google.com"
            )
        } catch (e: NoCredentialException) {
            Log.e(TAG, "$e")
            return if (filterAuthorizedAccounts) {
                requestGoogleSignIn(homeserver, filterAuthorizedAccounts = false)
            } else {
                BaseResponse.Error(code = LoginResultType.NO_GOOGLE_CREDENTIALS.name)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.e(TAG, "$e")
            return BaseResponse.Error()
        } catch (e: GetCredentialException) {
            Log.e(TAG, "${e.errorMessage}")
            return BaseResponse.Error(message = e.errorMessage?.toString() ?: "")
        }
    }

    actual suspend fun requestAppleSignIn(): LoginResultType {
        // Apple Sign-In on Android requires a WebView to present ASAuthorizationController
        // For simplicity, return FAILURE until implemented
        return LoginResultType.FAILURE
    }

    private suspend fun handleSignIn(
        homeserver: String,
        result: GetCredentialResponse,
        nonce: String,
        issuer: String
    ): BaseResponse<AugmySsoResponse> {
        if (lastNonce != nonce) return BaseResponse.Error()
        val credential = result.credential
        Log.d(TAG, "Received credential: $credential")
        return withContext(Dispatchers.IO) {
            when (credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            // Try login first
                            val loginResult = repository.loginWithGoogle(
                                homeserver,
                                idToken,
                                nonce = nonce,
                                issuer = issuer
                            )
                            if (loginResult.isSuccess) {
                                loginResult
                            } else {
                                // Fall back to registration
                                repository.registerWithGoogle(
                                    homeserver = homeserver,
                                    displayName = googleIdTokenCredential.displayName,
                                    idToken = idToken,
                                    nonce = nonce,
                                    issuer = issuer
                                )
                            }
                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e(TAG, "Invalid Google ID token: $e")
                            BaseResponse.Error()
                        }
                    } else BaseResponse.Error()
                }
                else -> BaseResponse.Error()
            }
        }
    }
}