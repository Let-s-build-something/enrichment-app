package koin

import augmy.interactive.com.BuildKonfig
import data.shared.DeveloperConsoleViewModel
import data.shared.SharedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.plugins.plugin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal expect fun httpClient(): HttpClient

/** Creates a new instance of http client with interceptor and authentication */
@OptIn(ExperimentalUuidApi::class)
internal fun httpClientFactory(
    sharedViewModel: SharedViewModel,
    developerViewModel: DeveloperConsoleViewModel?,
    json: Json
): HttpClient {
    return httpClient().config {
        defaultRequest {
            contentType(ContentType.Application.Json)

            host = developerViewModel?.hostOverride ?: BuildKonfig.HttpsHostName
            url {
                protocol = URLProtocol.HTTPS
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL

            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        ResponseObserver { response ->
            developerViewModel?.appendHttpLog(
                DeveloperUtils.processResponse(response)
            )
        }
        HttpResponseValidator {
            validateResponse { response ->
                try {
                    if (response.status == EXPIRED_TOKEN_CODE) {
                        println("Unauthorized - handle token refresh or log the user out")
                    }
                }catch (_: Exception) { }
            }
        }
    }.apply {
        plugin(HttpSend).intercept { request ->
            // add sensitive information only for our domains
            if(request.url.host == (developerViewModel?.hostOverride ?: BuildKonfig.HttpsHostName)) {
                request.headers.append(HttpHeaders.Authorization, "Bearer ${BuildKonfig.BearerToken}")
                request.headers.append(HttpHeaders.XRequestId, Uuid.random().toString())

                // assign current idToken
                sharedViewModel.currentUser.value?.idToken?.let { idToken ->
                    request.headers.append(HttpHeaders.IdToken, idToken)
                }
            }

            developerViewModel?.appendHttpLog(
                DeveloperUtils.processRequest(request)
            )
            execute(request)
        }
    }
}

/** Authorization type header with Firebase identification token */
val HttpHeaders.IdToken: String
    get() = "Id-Token"

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized

internal const val HttpDomain = "https://augmy.org"