package data.shared.sync

import augmy.interactive.shared.utils.DateUtils
import data.io.base.AppPing
import data.shared.SharedDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.mp.KoinPlatform

class DataService {
    companion object {
        private const val PING_EXPIRY_MS = 60_000 * 15
    }

    private val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val pingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastPingTime: Long = 0L
    private val mutex = Mutex()

    private var jobs = mutableListOf<String>()

    fun appendPing(ping: AppPing) {
        pingScope.launch(Dispatchers.Default) {
            mutex.withLock(ping.identifier.hashCode() + ping.type.hashCode()) {
                val time = DateUtils.now.toEpochMilliseconds()
                val calculatedDelay = if(lastPingTime == 0L) 0 else lastPingTime - time
                lastPingTime = lastPingTime.coerceAtLeast(time) + 300L

                if(calculatedDelay > 0) {
                    // obsolete ping
                    if(jobs.none { it == ping.identifier }) {
                        jobs.add(ping.identifier)

                        delay(calculatedDelay)

                        jobs.remove(ping.identifier)

                        sharedDataManager.pingStream.value = LinkedHashSet(sharedDataManager.pingStream.value).apply {
                            retainAll {
                                DateUtils.now.toEpochMilliseconds().minus(it.timestamp) < PING_EXPIRY_MS
                            }
                        }.plus(ping)
                    }
                }
            }
        }
    }
}