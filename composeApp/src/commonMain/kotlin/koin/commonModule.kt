package koin

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import data.io.matrix.room.event.serialization.DefaultLocalSerializerMappings
import data.io.matrix.room.event.serialization.createEventSerializersModule
import data.shared.DeveloperConsoleViewModel
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.SharedViewModel
import data.shared.appServiceModule
import data.shared.auth.authModule
import data.shared.developerConsoleModule
import data.shared.sync.dataSyncModule
import database.databaseModule
import database.file.FileAccess
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.overwriteWith
import net.folivo.trixnity.core.serialization.events.DefaultEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.conversation.components.audio.mediaProcessorModule
import ui.home.homeModule

/** Common module for the whole application */
@OptIn(ExperimentalCoilApi::class, ExperimentalSerializationApi::class)
internal val commonModule = module {
    if(currentPlatform != PlatformType.Jvm) includes(settingsModule)
    single { FileAccess() }
    single { SharedDataManager() }
    single<EventContentSerializerMappings> { DefaultEventContentSerializerMappings }
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            useArrayPolymorphism = true
            coerceInputValues = true
            encodeDefaults = true
            explicitNulls = false
            allowSpecialFloatingPointValues = true
            allowStructuredMapKeys = true
            prettyPrint = true
            namingStrategy = JsonNamingStrategy.SnakeCase
            serializersModule = createEventSerializersModule(
                DefaultEventContentSerializerMappings
            ).overwriteWith(DefaultLocalSerializerMappings)
        }
    }

    includes(databaseModule)
    includes(dataSyncModule)
    includes(authModule)
    viewModelOf(::SharedViewModel)
    includes(homeModule)
    includes(appServiceModule)
    includes(mediaProcessorModule)

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

    val sharedViewModel = KoinPlatform.getKoin().get<SharedViewModel>()
    val developerViewModel = (if(isDev) KoinPlatform.getKoin().get<DeveloperConsoleViewModel>() else null)?.also { vm ->
        single<DeveloperConsoleViewModel> { vm }
    }
    val json = KoinPlatform.getKoin().get<Json>()
    val httpClient = httpClientFactory(
        sharedViewModel = sharedViewModel,
        developerViewModel = developerViewModel,
        json = json
    )
    single<HttpClient> { httpClient }
    single<HttpClientEngine> { httpClient.engine }

    factory { SharedRepository(get<HttpClient>()) }
}

expect val settings: AppSettings

expect val secureSettings: SecureAppSettings
