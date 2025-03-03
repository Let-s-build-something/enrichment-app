package koin

import augmy.interactive.com.BuildKonfig
import base.utils.NetworkSpeed
import base.utils.speedInMbps
import data.shared.DeveloperConsoleViewModel
import data.shared.SharedViewModel
import data.shared.sync.DataSyncService.Companion.SYNC_INTERVAL
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
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
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
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
    var forceRefreshCountdown = 3

    return httpClient().config {
        defaultRequest {
            contentType(ContentType.Application.Json)

            host = developerViewModel?.hostOverride ?: BuildKonfig.HttpsHostName
            url {
                protocol = URLProtocol.HTTPS
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = SYNC_INTERVAL + 10_000
            socketTimeoutMillis = SYNC_INTERVAL + 10_000
            connectTimeoutMillis = 5_000
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY

            sanitizeHeader { header ->
                header == HttpHeaders.Authorization
                        || header == IdToken
                        || header == AccessToken
            }
        }
        ResponseObserver { response ->
            developerViewModel?.appendHttpLog(
                DeveloperUtils.processResponse(response)
            )

            val speedMbps = response.speedInMbps().roundToInt()
            sharedViewModel.updateNetworkConnectivity(
                networkSpeed = when {
                    speedMbps <= 1.0 -> NetworkSpeed.VerySlow
                    speedMbps <= 2.0 -> NetworkSpeed.Slow
                    speedMbps <= 5.0 -> NetworkSpeed.Moderate
                    speedMbps <= 10.0 -> NetworkSpeed.Good
                    else -> NetworkSpeed.Fast
                }.takeIf { speedMbps != 0 },
                isNetworkAvailable = true
            )
        }
        HttpResponseValidator {
            handleResponseException { cause, _ ->
                when (cause) {
                    is IOException, is UnresolvedAddressException -> {
                        sharedViewModel.updateNetworkConnectivity(isNetworkAvailable = false)
                        println("No network connection or server unreachable")
                    }
                    is ConnectTimeoutException, is SocketTimeoutException -> {
                        sharedViewModel.updateNetworkConnectivity(isNetworkAvailable = false)
                        println("Network timeout")
                    }
                }
            }
        }
    }.apply {
        plugin(HttpSend).intercept { request ->
            request.headers.append(HttpHeaders.XRequestId, Uuid.random().toString())

            // add sensitive information only for trusted domains
            when {
                request.url.toString().contains("/_matrix/") -> {
                    sharedViewModel.currentUser.value?.accessToken?.let { accessToken ->
                        request.headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                }
                request.url.host == (developerViewModel?.hostOverride ?: BuildKonfig.HttpsHostName) -> {
                    request.headers.append(HttpHeaders.Authorization, "Bearer ${BuildKonfig.BearerToken}")
                    sharedViewModel.currentUser.value?.idToken?.let { idToken ->
                        request.headers.append(IdToken, idToken)
                    }
                    sharedViewModel.currentUser.value?.accessToken?.let { accessToken ->
                        request.headers.append(AccessToken, accessToken)
                    }
                }
            }

            developerViewModel?.appendHttpLog(
                DeveloperUtils.processRequest(request)
            )
            val call = execute(request)

            // retry for 401 response
            if (call.response.status == EXPIRED_TOKEN_CODE
                && request.url.toString().contains("/_matrix/")
                && forceRefreshCountdown-- > 0
            ) {
                if(sharedViewModel.initUser(true)) {
                    request.headers[HttpHeaders.Authorization] = "Bearer ${sharedViewModel.currentUser.value?.accessToken}"
                    return@intercept execute(request)
                }
            }else forceRefreshCountdown = 3

            call
        }
    }
}

/** Authorization type header with Firebase identification token */
val IdToken: String
    get() = "Id-Token"

/** Authorization type header with Firebase identification token */
val AccessToken: String
    get() = "Access-Token"

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized

internal const val HttpDomain = "https://augmy.org"