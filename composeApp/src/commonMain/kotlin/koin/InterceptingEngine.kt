package koin

import base.utils.NetworkSpeed
import base.utils.speedInMbps
import data.shared.SharedDataManager
import data.shared.SharedModel
import data.shared.auth.AuthService
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.mp.KoinPlatform
import ui.dev.DeveloperConsoleModel
import utils.DeveloperUtils
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InterceptingEngine(
    private val engine: HttpClientEngine,
    private val authService: AuthService,
    private val dataManager: SharedDataManager,
    override val config: HttpClientEngineConfig = engine.config,
    override val coroutineContext: CoroutineContext = engine.coroutineContext,
    override val dispatcher: CoroutineDispatcher = engine.dispatcher
) : HttpClientEngine {

    override val supportedCapabilities = engine.supportedCapabilities

    private var forceRefreshCountdown = 3
    private val developerModel = KoinPlatform.getKoin().getOrNull<DeveloperConsoleModel>()
    private val sharedModel = KoinPlatform.getKoin().get<SharedModel>()

    @OptIn(ExperimentalUuidApi::class)
    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val interceptedId = Uuid.random().toString()
        val interceptedData = HttpRequestData(
            url = data.url,
            headers = Headers.build {
                appendAll(data.headers)
                set(HttpHeaders.UserAgent, "Augmy")
                set(HttpHeaders.XRequestId, interceptedId)
                set(HttpHeaders.Authorization, "Bearer ${dataManager.currentUser.value?.accessToken}")
            },
            method = data.method,
            body = data.body,
            attributes = data.attributes,
            executionContext = data.executionContext
        )

        developerModel?.appendHttpLog(
            DeveloperUtils.processRequest(interceptedData)
        )

        val response = engine.execute(interceptedData)

        developerModel?.appendHttpLog(
            DeveloperUtils.processResponse(
                id = interceptedId,
                response = response
            )
        )

        // sync has a very long timeout which would throw off this calculation
        val speedMbps = response.speedInMbps().roundToInt()
        sharedModel.updateNetworkConnectivity(
            networkSpeed = if (!interceptedData.url.toString().contains("/sync")) {
                when {
                    speedMbps <= 1.0 -> NetworkSpeed.VerySlow
                    speedMbps <= 2.0 -> NetworkSpeed.Slow
                    speedMbps <= 5.0 -> NetworkSpeed.Moderate
                    speedMbps <= 10.0 -> NetworkSpeed.Good
                    else -> NetworkSpeed.Fast
                }.takeIf { speedMbps != 0 }
            } else null,
            isNetworkAvailable = response.statusCode.value < 500
        )

        // retry for 401 response
        if (response.statusCode == EXPIRED_TOKEN_CODE
            && data.url.host.contains(dataManager.currentUser.value?.matrixHomeserver ?: ".;'][.")
            && !data.url.encodedPath.contains("/refresh")
            && forceRefreshCountdown-- > 0
        ) {
            val responseBody = response.body.toString()

            if(responseBody.contains("M_MISSING_TOKEN") || responseBody.contains("M_UNKNOWN_TOKEN")) {
                authService.setupAutoLogin(forceRefresh = false)
                if(dataManager.currentUser.value?.accessToken != null) {
                    return execute(HttpRequestData(
                        url = data.url,
                        headers = Headers.build {
                            appendAll(data.headers)
                            set(HttpHeaders.Authorization, "Bearer ${dataManager.currentUser.value?.accessToken}")
                            set(HttpHeaders.XRequestId, Uuid.random().toString())
                        },
                        method = data.method,
                        body = data.body,
                        attributes = data.attributes,
                        executionContext = data.executionContext
                    ))
                }
            }
        }else forceRefreshCountdown = 3

        return response
    }

    override fun close() {
        engine.close()
    }
}