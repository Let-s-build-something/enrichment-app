package data.shared.sync

import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ext.ifNull
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.social.UserVisibility
import data.shared.SharedDataManager
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.matrix.MatrixPagingMetaDao
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.onCancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.milliseconds

internal val dataSyncModule = module {
    factory { DataSyncHandler() }
    factory { DataSyncService() }
    single { DataSyncService() }
    single { DataService() }
}

class DataSyncService {
    private val logger = KotlinLogging.logger(name = "DataSyncServiceLogger")

    companion object {
        const val SYNC_INTERVAL = 60_000L
        private const val START_ANEW = false // for debug use
    }

    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val matrixPagingMetaDao: MatrixPagingMetaDao by KoinPlatform.getKoin().inject()
    private val conversationRoomDao: ConversationRoomDao by KoinPlatform.getKoin().inject()
    private val conversationMessageDao: ConversationMessageDao by KoinPlatform.getKoin().inject()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var homeserver: String? = null
    private var nextBatch: String? = null
    private var isRunning = false
    private val handler = DataSyncHandler()
    private val synMutex = Mutex()

    /** Begins the synchronization process and runs it over and over as long as the app is running or stopped via [stop] */
    fun sync(homeserver: String, delay: Long? = null) {
        if(!isRunning && homeserver.isNotBlank()) {
            this@DataSyncService.homeserver = homeserver
            isRunning = true

            syncScope.launch {
                this.coroutineContext.onCancel {
                    isRunning = false
                }

                synMutex.withLock {
                    if(START_ANEW && BuildKonfig.isDevelopment) {
                        matrixPagingMetaDao.removeAll()
                        conversationRoomDao.removeAll()
                        conversationMessageDao.removeAll()
                    }

                    sharedDataManager.matrixClient.value?.let { client ->
                        delay?.let { delay(it) }
                        if(sharedDataManager.currentUser.value?.isFullyValid == true) {
                            enqueue(client = client)
                        }else {
                            logger.debug { "User not fully valid, stopping." }
                            stop()
                        }
                    }.ifNull {
                        logger.debug { "Client is null, stopping." }
                        stop()
                    }
                }
            }
        }
    }

    fun stop() {
        if(isRunning) {
            handler.stop()
            isRunning = false
            syncScope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun CoroutineScope.enqueue(
        client: MatrixClient,
        homeserver: String? = this@DataSyncService.homeserver,
        since: String? = this@DataSyncService.nextBatch
    ) {
        val owner = sharedDataManager.currentUser.value?.matrixUserId
        if(homeserver == null || owner == null) {
            stop()
            return
        }

        val initialEntity = matrixPagingMetaDao.getByEntityId(
            entityId = "${homeserver}_$owner"
        )
        var prevBatch: String? = initialEntity?.currentBatch
        var currentBatch = since ?: initialEntity?.nextBatch

        client.api.sync.subscribe {
            handler.handle(
                response = it.syncResponse,
                owner = owner
            )
        }

        logger.debug { "enqueue, entityId: ${homeserver}_$owner" }
        client.api.sync.start(
            timeout = SYNC_INTERVAL.milliseconds,
            asUserId = UserId(owner),
            setPresence = when(sharedDataManager.currentUser.value?.configuration?.visibility) {
                UserVisibility.Online -> Presence.ONLINE
                UserVisibility.Invisible, UserVisibility.Offline -> Presence.OFFLINE
                else -> Presence.UNAVAILABLE
            },
            scope = this,
            getBatchToken = {
                (currentBatch ?: matrixPagingMetaDao.getByEntityId(entityId = "${homeserver}_$owner")?.nextBatch)
            },
            setBatchToken = { nextBatch ->
                this@DataSyncService.nextBatch = nextBatch
                matrixPagingMetaDao.insert(
                    MatrixPagingMetaIO(
                        entityId = "${homeserver}_$owner",
                        entityType = PagingEntityType.Sync.name,
                        nextBatch = nextBatch,
                        currentBatch = currentBatch,
                        prevBatch = prevBatch
                    )
                ).also {
                    logger.debug { "setBatchToken: $nextBatch, entityId: ${homeserver}_$owner" }
                }

                prevBatch = currentBatch
                currentBatch = nextBatch
            }
        )
    }
}
