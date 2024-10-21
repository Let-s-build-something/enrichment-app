package koin

import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import com.russhwolf.settings.Settings
import data.shared.SharedDataManager
import data.shared.SharedViewModel
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
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
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            useArrayPolymorphism = true
        }
    }
    single {
        NetworkFetcher.Factory(
            networkClient = { get<HttpClient>().asNetworkClient() }
        )
    }
    single {
        httpClientFactory(
            sharedViewModel = get<SharedViewModel>(),
            json = get()
        )
    }
}