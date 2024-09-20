package koin

import chat.enrichment.eu.SharedBuildConfig
import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import com.russhwolf.settings.Settings
import data.shared.SharedDataManager
import data.shared.SharedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.signInServiceModule

/** Common module for the whole application */
@OptIn(ExperimentalCoilApi::class)
internal val commonModule = module {
    includes(signInServiceModule())

    single { SharedDataManager() }
    single { Settings() }
    viewModelOf(::SharedViewModel)

    single {
        NetworkFetcher.Factory(
            networkClient = { get<HttpClient>().asNetworkClient() },
            cacheStrategy = { CacheStrategy() },
        )
    }
    single {
        HttpClient().apply {
            this.plugin(HttpSend).intercept { request ->
                val sharedDataManager = get<SharedDataManager>()

                request.bearerAuth(SharedBuildConfig.BearerToken)
                request.headers.append(
                    HttpHeaders.Authorization,
                    sharedDataManager.currentUser.value?.token ?: ""
                )

                val originalCall = execute(request)
                if (originalCall.response.status.value == EXPIRED_TOKEN_CODE.value) {
                    // add token refresh logic here
                    execute(request)
                } else {
                    originalCall
                }
            }
        }.config {
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
}

/** http response code indicating expired token */
internal val EXPIRED_TOKEN_CODE = HttpStatusCode.Unauthorized