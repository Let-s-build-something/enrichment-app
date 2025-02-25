package data.shared

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.EmojiSelectionDao
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import database.dao.matrix.InboundMegolmSessionDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.MegolmMessageIndexDao
import database.dao.matrix.OlmSessionDao
import database.dao.matrix.OutboundMegolmSessionDao
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomEventDao
import koin.DeveloperUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val developerConsoleModule = module {
    single<DeveloperConsoleDataManager> { DeveloperConsoleDataManager() }
    factory { DeveloperConsoleViewModel(
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get()
    ) }
    viewModelOf(::DeveloperConsoleViewModel)
}

class DeveloperConsoleDataManager {

    /** developer console size */
    val developerConsoleSize = MutableStateFlow(0f)

    /** Log information for past or ongoing http calls */
    val httpLogData = MutableStateFlow(DeveloperUtils.HttpLogData())

    /** Current host override if there is any */
    val hostOverride = MutableStateFlow<String?>(null)
}

/** Shared viewmodel for developer console */
class DeveloperConsoleViewModel(
    private val dataManager: DeveloperConsoleDataManager,
    private val networkItemDao: NetworkItemDao,
    private val conversationMessageDao: ConversationMessageDao,
    private val emojiSelectionDao: EmojiSelectionDao,
    private val pagingMetaDao: PagingMetaDao,
    private val conversationRoomDao: ConversationRoomDao,
    private val roomEventDao: RoomEventDao,
    private val presenceEventDao: PresenceEventDao,
    private val matrixPagingMetaDao: MatrixPagingMetaDao,
    private val olmSessionDao: OlmSessionDao,
    private val outboundMegolmSessionDao: OutboundMegolmSessionDao,
    private val inboundMegolmSessionDao: InboundMegolmSessionDao,
    private val megolmMessageIndexDao: MegolmMessageIndexDao
): SharedViewModel() {

    /** developer console size */
    val developerConsoleSize = dataManager.developerConsoleSize.asStateFlow()

    /** log data associated with this apps' http calls */
    val httpLogData = dataManager.httpLogData.asStateFlow()

    /** Current host override if there is any */
    val hostOverride
        get() = dataManager.hostOverride.value


    //======================================== functions ==========================================

    /** Changes the state of the developer console */
    fun changeDeveloperConsole(size: Float = developerConsoleSize.value) {
        dataManager.developerConsoleSize.value = size
    }

    /** Overrides current host */
    fun changeHost(host: String) {
        dataManager.hostOverride.value = host
    }

    fun deleteLocalData() {
        viewModelScope.launch {
            networkItemDao.removeAll()
            conversationMessageDao.removeAll()
            emojiSelectionDao.removeAll()
            pagingMetaDao.removeAll()
            conversationRoomDao.removeAll()
            roomEventDao.removeAll()
            presenceEventDao.removeAll()
            matrixPagingMetaDao.removeAll()
            olmSessionDao.removeAll()
            outboundMegolmSessionDao.removeAll()
            inboundMegolmSessionDao.removeAll()
            megolmMessageIndexDao.removeAll()
            super.logoutCurrentUser()
        }
    }

    /** appends new or updates existing http log */
    @OptIn(ExperimentalUuidApi::class)
    fun appendHttpLog(call: DeveloperUtils.HttpCall?) {
        if(call == null) return
        dataManager.httpLogData.value = DeveloperUtils.HttpLogData(
            id = Uuid.random().toString(),
            httpCalls = dataManager.httpLogData.value.httpCalls.apply {
                find { it.id == call.id }?.update(call).ifNull {
                    add(call)
                }
            }
        )
    }
}