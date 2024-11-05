package koin

import augmy.interactive.com.BuildKonfig
import data.shared.SharedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect fun httpClient(): HttpClient

/** Creates a new instance of http client with interceptor and authentication */
internal fun httpClientFactory(
    sharedViewModel: SharedViewModel,
    json: Json
): HttpClient {
    return httpClient().config {
        defaultRequest {
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Authorization, "Bearer ${BuildKonfig.BearerToken}")

            // assign current idToken
            sharedViewModel.currentUser.value?.idToken?.let { idToken ->
                headers.append(HttpHeaders.IdToken, idToken)
            }

            host = BuildKonfig.HttpsHostName
            url {
                protocol = URLProtocol.HTTPS
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    // Here you can save the log message to a file or other destination
                    println("HTTP --> $message")
                }
            }
            level = LogLevel.ALL

            sanitizeHeader { header -> header == HttpHeaders. Authorization }
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == EXPIRED_TOKEN_CODE) {
                    println("Unauthorized - handle token refresh or log the user out")
                }
            }
        }
    }
}

/** Authorization type header with Firebase identification token */
val HttpHeaders.IdToken: String
    get() = "Id-Token"

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized