package ui.login

import chat.enrichment.eu.SharedBuildConfig
import data.io.identity_platform.IdentityMessageType
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

/** module providing platform-specific sign in options */
actual fun signInServiceModule() = module {
    factoryOf(::DesktopSignInRepository)
    single<UserOperationService> {
        UserOperationService(getKoin().get())
    }
}

/** Repository for access to Google Cloud Identity Platform for the purpose of logging in and signing up the user */
class DesktopSignInRepository(private val httpClient: HttpClient) {

    /** Makes a request for identity tool sign up with email and password */
    suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? {
        return withContext(Dispatchers.IO) {
            val res = httpClient.post(
                urlString = "$identityToolUrl:signUp",
                block =  {
                    header("Content-Type", "application/json")
                    parameter("key", SharedBuildConfig.CloudWebApiKey)
                    parameter("returnSecureToken", true)
                    parameter("email", email)
                    parameter("password", password)
                }
            )
            println(res.bodyAsText())
            res.body()
        }
    }
}

actual class UserOperationService(private val repository: DesktopSignInRepository) {

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

    actual suspend fun signUpWithPassword(email: String, password: String): IdentityMessageType? {
        val res = repository.signUpWithPassword(email, password)

        return if(res?.localId != null) {
            IdentityMessageType.SUCCESS
        }else res?.error?.type
    }
}