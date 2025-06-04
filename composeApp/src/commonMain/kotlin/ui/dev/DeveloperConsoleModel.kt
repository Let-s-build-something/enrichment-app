package ui.dev

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import base.utils.openSinkFromUri
import data.io.app.SettingsKeys.KEY_STREAMING_DIRECTORY
import data.io.app.SettingsKeys.KEY_STREAMING_URL
import data.io.base.BaseResponse
import data.sensor.SensorDelay
import data.sensor.SensorEvent
import data.sensor.SensorEventListener
import data.sensor.getAllSensors
import data.shared.SharedModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import koin.DeveloperUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val developerConsoleModule = module {
    single<DeveloperConsoleDataManager> { DeveloperConsoleDataManager() }
    factory { DeveloperRepository() }
    factory {
        DeveloperConsoleModel(
            get(),
            get()
        )
    }
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
    private val repository: DeveloperRepository
): SharedModel() {
    private val _streamingUrlResponse = MutableStateFlow<BaseResponse<*>>(BaseResponse.Idle)
    private val _availableSensors = MutableStateFlow(listOf<SensorEventListener>())
    private val _activeSensors = MutableStateFlow(listOf<String>())
    private var localStreamJob: Job? = null
    private var remoteStreamJob: Job? = null
    private val streamChannel = Channel<String>(capacity = Channel.UNLIMITED)
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
    val streamLines = MutableStateFlow(listOf<String>())
    val isLocalStreamRunning = MutableStateFlow(false)


    //======================================== functions ==========================================

    init {
        viewModelScope.launch {
            streamingUrl = settings.getString(KEY_STREAMING_URL, "")
            streamingDirectory = settings.getString(KEY_STREAMING_DIRECTORY, "")

            getAllSensors()?.also {
                _availableSensors.value = it
            }

            activeSensors.collectLatest { sensors ->
                _availableSensors.value.forEach { sensor ->
                    sensor.listener = if (sensor.uid in sensors) {
                        { event ->
                            eventStreamListener.invoke(sensor, event)
                        }
                    }else null
                }
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

    fun stopRemoteStream() {
        remoteStreamJob?.cancel()
        _streamingUrlResponse.value = BaseResponse.Idle
    }

    var remoteStreamDelay = SensorDelay.Normal
    fun setupRemoteStream(uri: CharSequence) {
        if(uri.isBlank()) return
        _streamingUrlResponse.value = BaseResponse.Loading
        viewModelScope.launch {
            _streamingUrlResponse.value = repository.postStreamData(
                url = uri.toString(),
                body = ""
            ).also {
                if(it is BaseResponse.Success) {
                    streamingUrl = uri.toString()
                    settings.putString(KEY_STREAMING_URL, uri.toString())

                    remoteStreamJob = CoroutineScope(Dispatchers.IO).launch {
                        val buffer = mutableListOf<String>()

                        for (line in streamChannel) {
                            val step = when(remoteStreamDelay) {
                                SensorDelay.Slow -> 50
                                SensorDelay.Normal -> 20
                                SensorDelay.Fast -> 1
                            }

                            buffer.add(line)

                            if (buffer.size >= step && streamingUrl.isNotBlank()) {
                                repository.postStreamData(
                                    url = streamingUrl,
                                    body = buffer.joinToString("\n")
                                ).let {
                                    if(it is BaseResponse.Success) {
                                        buffer.clear()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun registerAllSensors() {
        viewModelScope.launch {
            _activeSensors.value = _availableSensors.value.mapNotNull { sensor ->
                if(_activeSensors.value.contains(sensor.uid)) {
                    null
                }else {
                    sensor.register()
                    sensor.uid
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
            _availableSensors.value.forEach {
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

            if(isLocalStreamRunning.value || _streamingUrlResponse.value is BaseResponse.Success) {
                streamChannel.trySend(newLine)
                streamLines.update {
                    it.toMutableList().apply {
                        add(0, newLine)
                    }
                }
            }
        }
    }

    fun stopLocalStream() {
        isLocalStreamRunning.value = false
        localStreamJob?.cancel()
        try {
            if(::sink.isInitialized) sink.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    lateinit var sink: BufferedSink
    fun setUpLocalStream(file: PlatformFile?) {
        if (file == null) return

        viewModelScope.launch {
            try {
                streamingDirectory = file.path
                settings.putString(KEY_STREAMING_DIRECTORY, streamingDirectory)
                isLocalStreamRunning.value = true

                localStreamJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sink = file.openSinkFromUri().buffer()

                        for (line in streamChannel) {
                            sink.writeUtf8(line)
                            sink.writeUtf8("\n")
                            sink.flush()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        delay(5000)
                        if (isActive && isLocalStreamRunning.value) {
                            setUpLocalStream(file)
                        }
                    } finally {
                        if (::sink.isInitialized) {
                            try {
                                sink.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportData(file: PlatformFile?) {
        if (file?.path == null) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sensorList = _availableSensors.value.filter { it.uid in _activeSensors.value }
                val outputFile = file.path.toPath()

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
            repository.clearAllDao()
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