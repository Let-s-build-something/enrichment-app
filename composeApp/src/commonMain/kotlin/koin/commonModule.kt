package koin

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkFetcher
import coil3.network.ktor.asNetworkClient
import com.russhwolf.settings.Settings
import data.shared.SharedDataManager
import data.shared.SharedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ui.login.signInServiceModule

/** initializes koin */
fun initKoin() {
    startKoin {
        modules(commonModule)
    }
}

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
        HttpClient().config {
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