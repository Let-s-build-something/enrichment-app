package koin

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import data.io.matrix.room.event.serialization.DefaultLocalSerializerMappings
import data.io.matrix.room.event.serialization.createEventSerializersModule
import data.shared.DeveloperConsoleModel
import data.shared.SharedDataManager
import data.shared.SharedModel
import data.shared.SharedRepository
import data.shared.appServiceModule
import data.shared.auth.AuthService
import data.shared.auth.authModule
import data.shared.auth.matrixRepositoryModule
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
    includes(matrixRepositoryModule)
    includes(authModule)
    viewModelOf(::SharedModel)
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

    single<HttpClient> {
        httpClientFactory(
            sharedModel = get<SharedModel>(),
            developerViewModel = if(isDev) get<DeveloperConsoleModel>() else null,
            json = get<Json>(),
            authService = get<AuthService>()
        )
    }
    single<HttpClientEngine> { get<HttpClient>().engine }

    factory { SharedRepository(get<HttpClient>()) }
}

expect val settings: AppSettings

expect val secureSettings: SecureAppSettings
