package ui.dev

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import base.utils.getDownloadsPath
import data.io.app.SettingsKeys.KEY_STREAMING_DIRECTORY
import data.io.app.SettingsKeys.KEY_STREAMING_URL
import data.io.base.BaseResponse
import data.sensor.SensorDelay
import data.sensor.SensorEvent
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
import io.github.vinceglb.filekit.core.PlatformFile
import koin.DeveloperUtils
import koin.secureSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
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
    private val json by lazy {
        Json {
            prettyPrint = false
            explicitNulls = false
        }
    }

    /** developer console size */
    val developerConsoleSize = dataManager.developerConsoleSize.asStateFlow()

    /** log data associated with this apps' http calls */
    val httpLogData = dataManager.httpLogData.asStateFlow()

    /** Current host override if there is any */
    val hostOverride
        get() = dataManager.hostOverride.value

    var streamingUrl = ""
    var streamingDirectory = ""
    val streamingUrlResponse = _streamingUrlResponse.asStateFlow()
    val availableSensors = _availableSensors.asStateFlow()
    val activeSensors = _activeSensors.asStateFlow()


    //======================================== functions ==========================================

    init {
        viewModelScope.launch {
            streamingUrl = settings.getString(KEY_STREAMING_URL, "")
            streamingDirectory = settings.getString(KEY_STREAMING_DIRECTORY, "")

            getAllSensors()?.also {
                _availableSensors.value = it
            }
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

    fun registerAllSensors() {
        viewModelScope.launch {
            _activeSensors.value = _availableSensors.value.mapNotNull {
                if(_activeSensors.value.contains(it.uid)) {
                    null
                }else {
                    it.register()
                    it.uid
                }
            }
        }
    }

    override fun onCleared() {
        _availableSensors.value.filter {
            it.uid in _activeSensors.value
        }.forEach {
            it.unregister()
        }
        super.onCleared()
    }

    fun unregisterAllSensors() {
        viewModelScope.launch {
            _availableSensors.value.filter {
                it.uid in _activeSensors.value
            }.forEach {
                it.unregister()
            }
            _activeSensors.value = listOf()
        }
    }

    fun resetAllSensors() {
        viewModelScope.launch {
            _availableSensors.value.forEach {
                it.data.value = listOf()
            }
        }
    }

    private var streamWriterJob: Job? = null
    private val streamChannel = Channel<String>(capacity = Channel.UNLIMITED)
    private val eventStreamListener: (sensor: SensorEventListener, event: SensorEvent) -> Unit = { sensor, event ->
        val sensorName = sensor.name

        val values = event.values?.toList()?.let {
            json.encodeToJsonElement(it)
        } ?: event.uiValues?.let {
            json.encodeToJsonElement(it)
        }

        if (values != null) {
            val jsonLine = buildJsonObject {
                put("sensor", sensorName)
                put("timestamp", event.timestamp)
                put("values", values)
            }
            val newLine = json.encodeToString(jsonLine)
            streamChannel.trySend(newLine)
            streamLines.update {
                it.toMutableList().apply {
                    add(0, newLine)
                }
            }
        }
    }

    val streamLines = MutableStateFlow(listOf<String>())

    fun stopStream() {
        _availableSensors.value.filter { it.uid in _activeSensors.value }.forEach {
            it.listener = null
        }
        streamWriterJob?.cancel()
        streamLines.value = listOf()
    }

    fun setUpStream(file: PlatformFile?) {
        if (file?.path == null) return
        streamWriterJob = CoroutineScope(Dispatchers.IO).launch {
            streamingDirectory = file.path?.toPath().toString()
            settings.putString(KEY_STREAMING_DIRECTORY, "")

            streamWriterJob = CoroutineScope(Dispatchers.IO).launch {
                FileSystem.SYSTEM.appendingSink(streamingDirectory.toPath()).buffer().use { sink ->
                    for (line in streamChannel) {
                        sink.writeUtf8(line)
                        sink.writeUtf8("\n")
                        sink.flush()
                    }
                }
            }

            _availableSensors.value.filter { it.uid in _activeSensors.value }.forEach { sensor ->
                sensor.listener = { event ->
                    eventStreamListener.invoke(sensor, event)
                }
            }
        }
    }

    fun exportData(file: PlatformFile?) {
        if (file?.path == null) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sensorList = _availableSensors.value.filter { it.uid in _activeSensors.value }
                val outputFile = file.path?.toPath() ?: getDownloadsPath().toPath().div(file.name)

                FileSystem.SYSTEM.write(outputFile) {
                    sensorList.forEach { sensor ->
                        val sensorName = sensor.name
                        val dataList = sensor.data.value

                        dataList.forEach { data ->
                            val values = data.values?.toList()?.let {
                                json.encodeToJsonElement(it)
                            } ?: data.uiValues?.let {
                                json.encodeToJsonElement(it)
                            }

                            if (values != null) {
                                val jsonLine = buildJsonObject {
                                    put("sensor", sensorName)
                                    put("timestamp", data.timestamp)
                                    put("values", values)
                                }

                                writeUtf8(json.encodeToString(jsonLine))
                                writeUtf8("\n")
                            }
                        }
                    }
                }

                sensorList.forEach {
                    it.data.value = listOf()
                }
            }
        }
    }

    fun registerSensor(
        sensor: SensorEventListener,
        delay: SensorDelay = SensorDelay.Slow
    ) {
        viewModelScope.launch {
            _activeSensors.update {
                it.toMutableSet().apply {
                    add(sensor.uid)
                }.toList()
            }
            sensor.register(sensorDelay = delay)
            if (streamWriterJob?.isActive == true) {
                sensor.listener = { event ->
                    eventStreamListener.invoke(sensor, event)
                }
            }
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
            sensor.listener = null
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