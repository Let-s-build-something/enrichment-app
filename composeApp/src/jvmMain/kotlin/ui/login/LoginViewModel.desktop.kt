package ui.login

import Chatenrichment.composeApp.BuildConfig.CLOUD_WEB_API_KEY
import com.russhwolf.settings.Settings
import data.io.identity_platform.IdentityMessageType
import data.io.identity_platform.IdentityRefreshToken
import data.io.identity_platform.IdentityUserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ui.home.HomeViewModel.Companion.SETTINGS_KEY_AUTO_EMAIL
import ui.home.HomeViewModel.Companion.SETTINGS_KEY_AUTO_PASSWORD

/** module providing platform-specific sign in options */
actual fun signInServiceModule() = module {
    factoryOf(::DesktopSignInRepository)
    single<UserOperationService> {
        UserOperationService(getKoin().get())
    }
}

class DesktopSignInRepository(private val httpClient: HttpClient) {

    /** Makes a request for identity tool sign in with email and password */
    suspend fun signInWithPassword(email: String, password: String): IdentityUserResponse? {
        return withContext(Dispatchers.IO) {
            val res = httpClient.post(
                urlString = "$identityToolUrl:signInWithPassword",
                block =  {
                    header("Content-Type", "application/json")
                    parameter("key", CLOUD_WEB_API_KEY)
                    parameter("returnSecureToken", true)
                    parameter("email", email)
                    parameter("password", password)
                }
            )
            println(res.bodyAsText())
            res.body()
        }
    }

    /** Makes a request for identity tool sign up with email and password */
    suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? {
        return withContext(Dispatchers.IO) {
            val res = httpClient.post(
                urlString = "$identityToolUrl:signUp",
                block =  {
                    header("Content-Type", "application/json")
                    parameter("key", CLOUD_WEB_API_KEY)
                    parameter("returnSecureToken", true)
                    parameter("email", email)
                    parameter("password", password)
                }
            )
            println(res.bodyAsText())
            res.body()
        }
    }

    /** Makes a request for a new refreshToken */
    suspend fun refreshToken(refreshToken: String): IdentityRefreshToken? {
        return withContext(Dispatchers.IO) {
            val res = httpClient.post(
                urlString = "$secureTokenUrl/v1/token",
                block =  {
                    header("Content-Type", "application/json")
                    parameter("key", CLOUD_WEB_API_KEY)
                    parameter("grant_type", "refresh_token")
                    parameter("refreshToken", refreshToken)
                }
            )
            println(res.bodyAsText())
            res.body()
        }
    }
}

actual class UserOperationService(
    private val repository: DesktopSignInRepository,
    private val settings: Settings = Settings()
) {

    actual val availableOptions = listOf<SingInServiceOption>()

    actual suspend fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean,
        webClientId: String
    ): LoginResultType {
        return LoginResultType.FAILURE
    }

    actual suspend fun requestAppleSignIn(): LoginResultType {
        return LoginResultType.FAILURE
    }

    actual suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? {
        val res = repository.signUpWithPassword(email, password)

        return if(res?.error?.type == IdentityMessageType.EMAIL_EXISTS) {
            signInWithPassword(email, password)
        }else res
    }

    actual suspend fun signInWithPassword(email: String, password: String): IdentityUserResponse? {
        val res = repository.signInWithPassword(email, password)

        if(res?.email != null) {
            withContext(Dispatchers.Default) {
                settings.putString(SETTINGS_KEY_AUTO_EMAIL, email)
                settings.putString(SETTINGS_KEY_AUTO_PASSWORD, password)
            }
        }
        return res
    }

    actual suspend fun refreshToken(refreshToken: String): IdentityRefreshToken? {
        return repository.refreshToken(refreshToken)
    }
}