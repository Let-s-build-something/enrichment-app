package data.shared.auth

import base.utils.platformPathsModule
import data.io.matrix.auth.local.AuthItem
import data.shared.sync.DataSyncService.Companion.SYNC_INTERVAL
import database.factory.CreateRepositoriesModule
import database.factory.SecretByteArray
import database.factory.convertSecretByteArrayModule
import database.factory.platformCreateRepositoriesModuleModule
import database.factory.platformGetSecretByteArrayKey
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Url
import koin.httpClientConfig
import korlibs.io.util.getOrNullLoggingError
import kotlinx.serialization.Serializable
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.MatrixClientConfiguration.CacheExpireDurations
import net.folivo.trixnity.client.createTrixnityDefaultModuleFactories
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val log = KotlinLogging.logger {}

val matrixRepositoryModule = module {
    includes(platformCreateRepositoriesModuleModule())
    includes(platformGetSecretByteArrayKey())
    includes(convertSecretByteArrayModule())
    includes(platformPathsModule())
}

class MatrixClientFactory(
    private val httpClientEngine: HttpClientEngine,
    private val getLoginInfo: suspend (MatrixClientServerApiClient) -> Result<LoginInfo>,
    private val saveDatabasePassword: suspend (userId: String, databaseKey: SecretByteArray?) -> Unit
) {
    private val repositoriesModuleCreation: CreateRepositoriesModule = KoinPlatform.getKoin().get()

    suspend fun initializeMatrixClient(credentials: AuthItem): MatrixClient? {
        println("kostka_test, initializeMatrixClient, userId: ${credentials.userId}")
        return if(credentials.userId != null) {
            (if(credentials.databasePassword != null) {
                MatrixClient.fromStore(
                    repositoriesModule = loadRepositoriesModuleOrThrow(
                        UserId(credentials.userId),
                        credentials.databasePassword
                    ),
                    mediaStore = InMemoryMediaStore(),
                    configuration = {
                        configureClient(credentials.deviceId)
                    },
                ).getOrNullLoggingError()
            }else null) ?: MatrixClient.loginWith(
                baseUrl = Url("https://${credentials.homeserver}"),
                getLoginInfo = {
                    getLoginInfo(it)
                },
                repositoriesModuleFactory = { loginInfo ->
                    createRepositoriesModuleOrThrow(loginInfo.userId).also {
                        saveDatabasePassword(loginInfo.userId.full, it.databaseKey)
                    }.module
                },
                mediaStoreFactory = { InMemoryMediaStore() },
                configuration = {
                    configureClient(credentials.deviceId)
                }
            ).getOrNullLoggingError()
        }else null
    }

    private fun MatrixClientConfiguration.configureClient(name: String?) {
        this.name = name
        lastRelevantEventFilter = { roomEvent ->
            roomEvent is RoomEvent.MessageEvent<*>
        }
        storeTimelineEventContentUnencrypted = false
        modulesFactories = createTrixnityDefaultModuleFactories()
        httpClientEngine = this@MatrixClientFactory.httpClientEngine
        cacheExpireDurations = CacheExpireDurations.default(30.minutes)
        syncLoopTimeout = SYNC_INTERVAL.milliseconds
        httpClientConfig = {
            httpClientConfig(sharedModel = KoinPlatform.getKoin().get())
        }
    }

    private suspend fun createRepositoriesModuleOrThrow(
        userId: UserId,
    ): CreateRepositoriesModule.CreateResult {
        log.debug { "create repositories module" }
        val repositoriesModule = try {
            repositoriesModuleCreation.create(userId)
        } catch (exc: Exception) {
            exc.printStackTrace()
            if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
            else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
        }
        return repositoriesModule
    }

    private suspend fun loadRepositoriesModuleOrThrow(
        userId: UserId,
        databasePassword: SecretByteArray?,
    ): Module {
        log.debug { "load repositories module" }
        val repositoriesModule = try {
            repositoriesModuleCreation.load(userId, databasePassword)
        } catch (e: Exception) {
            e.printStackTrace()
            if (isLocked(e)) throw MatrixClientInitializationException.DatabaseLockedException()
            else throw MatrixClientInitializationException.DatabaseAccessException(e.message)
        }
        return repositoriesModule
    }

    private fun isLocked(exc: Throwable): Boolean =
        exc.cause?.message?.contains("locked") == true || exc.cause?.let { isLocked(it) } ?: false
}

@Serializable
sealed interface MatrixClientInitializationException {
    @Serializable
    data class DatabaseAccessException(override val message: String? = null) : MatrixClientInitializationException,
        RuntimeException(message)

    @Serializable
    data class DatabaseLockedException(override val message: String? = null) : MatrixClientInitializationException,
        RuntimeException(message)
}