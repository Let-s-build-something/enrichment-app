package koin

import augmy.interactive.com.BuildKonfig
import base.utils.NetworkSpeed
import base.utils.speedInMbps
import data.shared.DeveloperConsoleModel
import data.shared.SharedModel
import data.shared.sync.DataSyncService.Companion.SYNC_INTERVAL
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
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
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.MatrixServerException
import org.koin.mp.KoinPlatform
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

internal expect fun httpClient(): HttpClient

/** Creates a new instance of http client with interceptor and authentication */
@OptIn(ExperimentalUuidApi::class)
internal fun httpClientFactory(
    sharedModel: SharedModel,
    developerViewModel: DeveloperConsoleModel?,
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
        install(HttpSend)
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpRequestRetry) {
            retryIf { httpRequest, httpResponse ->
                (httpResponse.status == HttpStatusCode.TooManyRequests)
                    .also { if (it) log.warn { "rate limit exceeded for ${httpRequest.method} ${httpRequest.url}" } }
            }
            retryOnExceptionIf { _, throwable ->
                (throwable is MatrixServerException && throwable.statusCode == HttpStatusCode.TooManyRequests)
                    .also {
                        if (it) {
                            log.warn(if (log.isDebugEnabled()) throwable else null) { "rate limit exceeded" }
                        }
                    }
            }
            exponentialDelay(maxDelayMs = 30_000, respectRetryAfterHeader = true)
        }
        httpClientConfig(sharedModel = sharedModel, autologin = false)
        install(HttpSend)
    }.apply {
        plugin(HttpSend).intercept { request ->
            request.headers.append(HttpHeaders.XRequestId, Uuid.random().toString())

            developerViewModel?.appendHttpLog(
                DeveloperUtils.processRequest(request)
            )
            val call = execute(request)

            // retry for 401 response
            if (call.response.status == EXPIRED_TOKEN_CODE
                && request.url.toString().contains("/_matrix/")
                && forceRefreshCountdown-- > 0
            ) {
                if(sharedModel.initUser(true)) {
                    request.headers[HttpHeaders.Authorization] = "Bearer ${sharedModel.currentUser.value?.accessToken}"
                    return@intercept execute(request)
                }
            }else forceRefreshCountdown = 3

            call
        }
    }
}

fun HttpClientConfig<*>.httpClientConfig(
    sharedModel: SharedModel,
    autologin: Boolean = true
) {
    val developerViewModel = KoinPlatform.getKoin().getOrNull<DeveloperConsoleModel>()

    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.BODY

        sanitizeHeader { header ->
            header == HttpHeaders.Authorization
                    || header == IdToken
                    || header == AccessToken
        }
    }
    install(HttpRequestRetry) {
        this.retryIf { request, response ->
            !request.headers.contains(RetryAttempt) && response.status == EXPIRED_TOKEN_CODE
        }
        modifyRequest { request ->
            when {
                request.url.toString().contains("/_matrix/") -> {
                    sharedModel.currentUser.value?.accessToken?.let { accessToken ->
                        request.headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                }
                request.url.host == (developerViewModel?.hostOverride ?: BuildKonfig.HttpsHostName) -> {
                    request.headers.append(HttpHeaders.Authorization, "Bearer ${BuildKonfig.BearerToken}")
                    sharedModel.currentUser.value?.idToken?.let { idToken ->
                        request.headers.append(IdToken, idToken)
                    }
                    sharedModel.currentUser.value?.accessToken?.let { accessToken ->
                        request.headers.append(AccessToken, accessToken)
                    }
                }
            }
            request.headers.append(
                RetryAttempt,
                (request.headers[RetryAttempt]?.toIntOrNull()?.plus(1) ?: 1).toString()
            )
        }
    }
    HttpResponseValidator {
        handleResponseException { cause, _ ->
            when (cause) {
                is IOException, is UnresolvedAddressException -> {
                    sharedModel.updateNetworkConnectivity(isNetworkAvailable = false)
                    println("No network connection or server unreachable")
                }
                is ConnectTimeoutException, is SocketTimeoutException -> {
                    sharedModel.updateNetworkConnectivity(isNetworkAvailable = false)
                    println("Network timeout")
                }
            }
        }
    }
    defaultRequest {
        when {
            url.toString().contains(sharedModel.currentUser.value?.matrixHomeserver ?: ".;'][.") -> {
                sharedModel.currentUser.value?.accessToken?.let { accessToken ->
                    headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }
            url.host == (developerViewModel?.hostOverride ?: BuildKonfig.HttpsHostName) -> {
                headers.append(HttpHeaders.Authorization, "Bearer ${BuildKonfig.BearerToken}")
                sharedModel.currentUser.value?.idToken?.let { idToken ->
                    headers.append(IdToken, idToken)
                }
                sharedModel.currentUser.value?.accessToken?.let { accessToken ->
                    headers.append(AccessToken, accessToken)
                }
            }
        }
    }
    ResponseObserver { response ->
        developerViewModel?.appendHttpLog(
            DeveloperUtils.processResponse(response)
        )

        // sync has a very long timeout which would throw off this calculation
        if(!response.request.url.toString().contains("/sync")) {
            val speedMbps = response.speedInMbps().roundToInt()
            sharedModel.updateNetworkConnectivity(
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
    }
}

/** Authorization type header with Firebase identification token */
val IdToken: String
    get() = "Id-Token"

/** Authorization type header with Firebase identification token */
val AccessToken: String
    get() = "Access-Token"

val RetryAttempt: String
    get() = "Retry-Attempt"

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized

internal const val HttpDomain = "https://augmy.org"