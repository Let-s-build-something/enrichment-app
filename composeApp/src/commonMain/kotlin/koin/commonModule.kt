@file:OptIn(ExperimentalSettingsApi::class)

package koin

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import data.shared.DeveloperConsoleViewModel
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.SharedViewModel
import data.shared.appServiceModule
import data.shared.developerConsoleModule
import database.databaseModule
import database.file.FileAccess
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


/** Common module for the whole application */
@OptIn(ExperimentalCoilApi::class)
internal val commonModule = module {
    if(currentPlatform != PlatformType.Jvm) includes(settingsModule)
    single { FileAccess() }
    single { SharedDataManager() }
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

    includes(databaseModule)
    viewModelOf(::SharedViewModel)
    includes(ui.home.homeModule)
    includes(appServiceModule)

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
        if(it) this@module.includes(developerConsoleModule)
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

expect val settings: FlowSettings
