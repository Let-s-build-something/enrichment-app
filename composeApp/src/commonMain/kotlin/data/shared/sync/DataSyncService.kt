package data.shared.sync

import data.io.matrix.SyncResponse
import data.shared.SharedDataManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import ui.login.safeRequest

internal val dataSyncModule = module {
    factory { DataSyncService() }
    single { DataSyncService() }
}

class DataSyncService {
    companion object {
        const val SYNC_INTERVAL = 60_000L
    }

    private val httpClient: HttpClient by KoinPlatform.getKoin().inject()
    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var homeserver: String? = null
    private var nextBatch: String? = null
    private var isRunning = false

    /** Begins the synchronization process and runs it over and over as long as the app is running or stopped via [stop] */
    fun sync(homeserver: String) {
        this.homeserver = homeserver
        if(!isRunning) {
            isRunning = true
            syncScope.launch {
                enqueue()
            }
        }
    }

    fun stop() {
        isRunning = false
    }

    private suspend fun enqueue(
        homeserver: String? = this.homeserver,
        nextBatch: String? = this.nextBatch
    ) {
        if(homeserver == null) return

        httpClient.safeRequest<SyncResponse> {
            get(urlString = "https://$homeserver/_matrix/client/v3/sync?timeout=$SYNC_INTERVAL" +
                    if(nextBatch != null) "&since=$nextBatch" else ""
            )
        }.let { syncResponse ->
            syncResponse.success?.data?.nextBatch?.let { nextBatch ->
                this@DataSyncService.nextBatch = nextBatch
                if(isRunning) {
                    syncResponse.success?.data?.let { data ->
                        // TODO process
                    }

                    enqueue()
                }
            }
        }
    }
}
