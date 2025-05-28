package ui.dev

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import data.io.app.SettingsKeys.KEY_STREAMING_URL
import data.io.base.BaseResponse
import data.sensor.SensorDelay
import data.sensor.SensorEventListener
import data.sensor.getAllSensors
import data.shared.SharedModel
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.EmojiSelectionDao
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.PresenceEventDao
import koin.DeveloperUtils
import koin.secureSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val developerConsoleModule = module {
    single<DeveloperConsoleDataManager> { DeveloperConsoleDataManager() }
    factory { DeveloperConsoleModel(
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get()
    ) }
    viewModelOf(::DeveloperConsoleModel)
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
class DeveloperConsoleModel(
    private val dataManager: DeveloperConsoleDataManager,
    private val networkItemDao: NetworkItemDao,
    private val conversationMessageDao: ConversationMessageDao,
    private val emojiSelectionDao: EmojiSelectionDao,
    private val pagingMetaDao: PagingMetaDao,
    private val conversationRoomDao: ConversationRoomDao,
    private val presenceEventDao: PresenceEventDao,
    private val matrixPagingMetaDao: MatrixPagingMetaDao
): SharedModel() {
    private val _streamingUrlResponse = MutableStateFlow<BaseResponse<*>>(BaseResponse.Idle)
    private val _availableSensors = MutableStateFlow(listOf<SensorEventListener>())
    private val _activeSensors = MutableStateFlow(listOf<String>())

    /** developer console size */
    val developerConsoleSize = dataManager.developerConsoleSize.asStateFlow()

    /** log data associated with this apps' http calls */
    val httpLogData = dataManager.httpLogData.asStateFlow()

    /** Current host override if there is any */
    val hostOverride
        get() = dataManager.hostOverride.value

    var streamingUrl = ""
    val streamingUrlResponse = _streamingUrlResponse.asStateFlow()
    val availableSensors = _availableSensors.asStateFlow()
    val activeSensors = _activeSensors.asStateFlow()


    //======================================== functions ==========================================

    init {
        viewModelScope.launch {
            streamingUrl = settings.getString(KEY_STREAMING_URL, "")
            println("kostka_test, sensors: ${getAllSensors()?.also {
                _availableSensors.value = it
            }}")
        }
    }

    /** Changes the state of the developer console */
    fun changeDeveloperConsole(size: Float = developerConsoleSize.value) {
        dataManager.developerConsoleSize.value = size
    }

    /** Overrides current host */
    fun changeHost(host: CharSequence) {
        dataManager.hostOverride.value = host.toString().takeIf { it.isNotBlank() }
    }

    fun selectStreamingUrl(uri: CharSequence) {
        viewModelScope.launch {
            streamingUrl = uri.toString()
            settings.getString(uri.toString(), "")
        }
    }

    fun registerSensor(
        sensor: SensorEventListener,
        delay: SensorDelay
    ) {
        viewModelScope.launch {
            _activeSensors.update {
                it.toMutableSet().apply {
                    add(sensor.uid)
                }.toList()
            }
            sensor.register(sensorDelay = delay)
        }
    }

    fun changeSensorDelay(sensor: SensorEventListener, delay: SensorDelay) {
        if (_activeSensors.value.contains(sensor.uid)) {
            viewModelScope.launch {
                sensor.unregister()
                sensor.register(sensorDelay = delay)
            }
        }
    }

    fun unRegisterSensor(sensor: SensorEventListener) {
        viewModelScope.launch {
            _activeSensors.update {
                it.toMutableList().apply {
                    remove(sensor.uid)
                }
            }
            sensor.unregister()
        }
    }

    fun deleteLocalData() {
        viewModelScope.launch {
            networkItemDao.removeAll()
            conversationMessageDao.removeAll()
            emojiSelectionDao.removeAll()
            conversationRoomDao.removeAll()
            presenceEventDao.removeAll()
            pagingMetaDao.removeAll()
            matrixPagingMetaDao.removeAll()
            secureSettings.clear(force = true)
            sharedDataManager.matrixClient.value?.clearCache()
            sharedDataManager.matrixClient.value?.clearMediaCache()
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