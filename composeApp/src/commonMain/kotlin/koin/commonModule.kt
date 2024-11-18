package koin

import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import com.russhwolf.settings.Settings
import data.shared.DeveloperConsoleViewModel
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.SharedViewModel
import data.shared.appServiceModule
import data.shared.developerConsoleModule
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Common module for the whole application */
@OptIn(ExperimentalCoilApi::class)
internal val commonModule = module {
    includes(ui.home.homeModule)
    includes(appServiceModule)

    single { SharedDataManager() }
    single { Settings() }
    viewModelOf(::SharedViewModel)

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            useArrayPolymorphism = true

            encodeDefaults = true
            allowSpecialFloatingPointValues = true
            allowStructuredMapKeys = true
            prettyPrint = true
        }
    }
    single {
        NetworkFetcher.Factory(
            networkClient = { get<HttpClient>().asNetworkClient() }
        )
    }

    val isDev = try {
        Firebase.auth.currentUser?.email?.endsWith("@augmy.org") == true
    }catch (e: NotImplementedError) {
        true // enabled on all JVM devices for now as there is no email getter
    }.also {
        if(it) includes(developerConsoleModule)
    }
    single {
        httpClientFactory(
            sharedViewModel = get<SharedViewModel>(),
            developerViewModel = if(isDev) get<DeveloperConsoleViewModel>() else null,
            json = get()
        )
    }

    factory { SharedRepository(get<HttpClient>()) }
}