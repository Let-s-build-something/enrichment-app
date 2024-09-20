package koin

import chat.enrichment.eu.BuildKonfig
import data.shared.SharedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Creates a new instance of http client with interceptor and authentication */
internal fun httpClientFactory(sharedViewModel: SharedViewModel): HttpClient {
    return HttpClient().apply {
        this.plugin(HttpSend).intercept { request ->
            /* assign token variable from firebase and keep it fresh
            sharedViewModel.currentUser.value?.token?.let { token ->
                request.headers.append(HttpHeaders.Authorization, token)
            }*/

            val originalCall = execute(request)
            if (originalCall.response.status.value == EXPIRED_TOKEN_CODE.value) {
                // add token refresh logic here
                execute(request)
            } else {
                originalCall
            }.also {
                println("HTTP ${it.request.url}, RESPONSE: ${it.response.status}: ${it.response.bodyAsText()}")
            }
        }
    }.config {
        defaultRequest {
            contentType(ContentType.Application.Json)
            host = BuildKonfig.HttpsHostName
            url {
                protocol = URLProtocol.HTTPS
            }
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized