package koin

import data.shared.DeveloperConsoleModel
import data.shared.SharedDataManager
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
import kotlin.coroutines.CoroutineContext

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
    private val developerViewModel = KoinPlatform.getKoin().getOrNull<DeveloperConsoleModel>()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        developerViewModel?.appendHttpLog(
            DeveloperUtils.processRequest(data)
        )

        val response = engine.execute(data)

        // retry for 401 response
        if (response.statusCode == EXPIRED_TOKEN_CODE
            && data.url.host.contains(dataManager.currentUser.value?.matrixHomeserver ?: ".;'][.")
            && !data.url.encodedPath.contains("/refresh")
            && forceRefreshCountdown-- > 0
        ) {
            val responseBody = response.body.toString()

            if(responseBody.contains("M_MISSING_TOKEN") || responseBody.contains("M_UNKNOWN_TOKEN")) {
                authService.setupAutoLogin(forceRefresh = true)
                if(dataManager.currentUser.value?.accessToken != null) {
                    return execute(HttpRequestData(
                        url = data.url,
                        headers = Headers.build {
                            appendAll(data.headers)
                            set(HttpHeaders.Authorization, "Bearer ${dataManager.currentUser.value?.accessToken}")
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