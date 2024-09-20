package koin

import chat.enrichment.eu.SharedBuildConfig
import data.shared.SharedDataManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Creates a new instance of http client with interceptor and authentication */
internal fun httpClientFactory(sharedDataManager: SharedDataManager): HttpClient {
    return HttpClient().apply {
        this.plugin(HttpSend).intercept { request ->
            request.bearerAuth(SharedBuildConfig.BearerToken)

            sharedDataManager.currentUser.value?.token?.let { token ->
                request.headers.append(HttpHeaders.Authorization, token)
            }

            val originalCall = execute(request)
            if (originalCall.response.status.value == EXPIRED_TOKEN_CODE.value) {
                // add token refresh logic here
                execute(request)
            } else {
                originalCall
            }
        }
    }.config {
        defaultRequest {
            host = SharedBuildConfig.HttpsHostName
            url {
                protocol = URLProtocol.HTTPS
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized