package ui.login

import augmy.interactive.com.BuildKonfig
import data.io.identity_platform.IdentityUserResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        useArrayPolymorphism = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
        allowSpecialFloatingPointValues = true
        allowStructuredMapKeys = true
        prettyPrint = true
    }

    @kotlinx.serialization.Serializable
    data class SignUpBody(
        val email: String,
        val password: String,
        val returnSecureToken: Boolean = true
    )

    /** Makes a request for identity tool sign up with email and password */
    suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? {
        return withContext(Dispatchers.IO) {
            json.decodeFromString<IdentityUserResponse?>(
                httpClient.safeRequestError<String> {
                    post(
                        urlString = "$identityToolUrl:signUp",
                        block =  {
                            header("Content-Type", "application/json")
                            parameter("key", BuildKonfig.CloudWebApiKey)
                            setBody(
                                SignUpBody(
                                    email = email,
                                    password = password
                                )
                            )
                        }
                    )
                } ?: ""
            )
        }
    }

    /** Makes a request for identity tool sign up with email and password */
    suspend fun deleteAccount(idToken: String): IdentityUserResponse? {
        return withContext(Dispatchers.IO) {
            json.decodeFromString<IdentityUserResponse?>(
                httpClient.safeRequest<String> {
                    post(
                        urlString = "https://www.googleapis.com/identitytoolkit/v3/relyingparty/deleteAccount?key=${BuildKonfig.CloudWebApiKey}",
                        block =  {
                            header("Content-Type", "application/json")
                            setBody(JsonObject(mapOf("idToken" to JsonPrimitive(idToken))))
                        }
                    )
                }.success?.data ?: ""
            )
        }
    }
}

actual class UserOperationService(private val repository: DesktopSignInRepository) {

    actual suspend fun requestGoogleSignIn(filterAuthorizedAccounts: Boolean): LoginResultType {
        return LoginResultType.FAILURE
    }

    actual suspend fun requestAppleSignIn(): LoginResultType {
        return LoginResultType.FAILURE
    }

    actual suspend fun signUpWithPassword(
        email: String,
        password: String,
        deleteRightAfter: Boolean
    ): IdentityUserResponse? {
        val res = repository.signUpWithPassword(email, password)
        if(deleteRightAfter) {
            res?.idToken?.let {
                repository.deleteAccount(it)
            }
        }

        println("kostka_test, signUpWithPassword: $res")
        return res
    }
}