package ui.dev

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import base.utils.openSinkFromUri
import data.io.app.SettingsKeys.KEY_STREAMING_DIRECTORY
import data.io.app.SettingsKeys.KEY_STREAMING_URL
import data.io.base.BaseResponse
import data.io.experiment.ExperimentIO
import data.io.experiment.ExperimentSetValue
import data.sensor.SensorDelay
import data.sensor.SensorEvent
import data.sensor.SensorEventListener
import data.sensor.getAllSensors
import data.shared.SharedModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import korlibs.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
import ui.dev.experiment.experimentModule
import utils.DeveloperUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val developerConsoleModule = module {
    single<DeveloperConsoleDataManager> { DeveloperConsoleDataManager() }
    factory { DeveloperConsoleRepository() }
    factory {
        DeveloperConsoleModel(get(), get())
    }
    viewModelOf(::DeveloperConsoleModel)
    includes(experimentModule)
}


/** Shared viewmodel for developer console */
class DeveloperConsoleModel(
    private val dataManager: DeveloperConsoleDataManager,
    private val repository: DeveloperConsoleRepository
): SharedModel() {

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

    val httpLogFilter = dataManager.httpLogFilter.asStateFlow()

    /** log data associated with this apps' http calls */
    val httpLogData = dataManager.httpLogData.combine(dataManager.httpLogFilter) { logs, filter ->
        (if (filter.first.isBlank()) {
            logs.httpCalls
        }else logs.httpCalls.filter {
            val input = filter.first.lowercase()

            it.url?.lowercase()?.contains(input) == true
                    || it.requestBody?.lowercase()?.contains(filter.first) == true
                    || it.responseBody?.lowercase()?.contains(filter.first) == true
                    || it.method?.value?.lowercase()?.contains(filter.first) == true
                    || it.headers?.any { h -> h.lowercase().contains(filter.first) } == true
                    || it.id.lowercase().contains(filter.first)
        }).sortedWith(
            if(filter.second) {
                compareBy { it.createdAt }
            }else compareByDescending { it.createdAt }
        )
    }

    val logFilter = dataManager.logFilter.asStateFlow()

    /** log data associated with this apps' http calls */
    internal val logData = dataManager.logs.combine(dataManager.logFilter) { logs, filter ->
        logs.filter {
            val input = filter.first.lowercase()

            (filter.first.isBlank() || it.message?.toString()?.lowercase()?.contains(input) == true)
                    && (filter.third == null || it.level == filter.third)
        }.sortedWith(
            if(filter.second) {
                compareBy { it.timestamp }
            }else compareByDescending { it.timestamp }
        )
    }

    /** Current host override if there is any */
    val hostOverride
        get() = dataManager.hostOverride.value

    var streamingUrl = ""
    var streamingDirectory = ""
    val streamingUrlResponse = dataManager.streamingUrlResponse.asStateFlow()
    val availableSensors = _availableSensors.asStateFlow()
    val activeSensors = _activeSensors.asStateFlow()
    val streamLines = MutableStateFlow(listOf<String>())
    val isLocalStreamRunning = MutableStateFlow(false)
    val experimentsToShow = dataManager.experimentsToShow.asStateFlow()
    private val activeExperimentScopes = hashMapOf<String, Job>()


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
                            streamEvent(sensor, event)
                        }
                    }else null
                }
            }
        }
        viewModelScope.launch {
            dataManager.activeExperiments.collectLatest { list ->
                list.forEach { uid ->
                    val experiment = dataManager.experiments.value.firstOrNull { it.data.uid == uid }
                    val interval = (experiment?.data?.displayFrequency as? ExperimentIO.DisplayFrequency.Constant)?.delaySeconds

                    if (interval != null && activeExperimentScopes[uid] == null) {
                        val newJob = Job()
                        CoroutineScope(newJob).launch(Dispatchers.Default) {
                            while (isActive) {
                                dataManager.experimentsToShow.update { prev ->
                                    prev.plus(experiment).distinctBy { it.data.uid }
                                }
                                delay(interval * 1000)
                            }
                        }
                        activeExperimentScopes[uid] = newJob
                    }
                }
                activeExperimentScopes.keys.filter { it !in list }.forEach {
                    activeExperimentScopes[it]?.cancel()
                    activeExperimentScopes.remove(it)
                }
            }
        }
    }

    fun reportExperimentValues(
        experiment: ExperimentIO,
        values: List<ExperimentSetValue>,
        customValue: String?
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            if (experiment.displayFrequency !is ExperimentIO.DisplayFrequency.Permanent) {
                dataManager.experimentsToShow.update { prev ->
                    prev.filter { it.data.uid != experiment.uid }
                }
            }
            streamSetValues(experiment, values, customValue)
        }
    }

    /** Searches for the input string in the body and headers and sorts the data set */
    fun filterHttpLogs(
        input: String = dataManager.httpLogFilter.value.first,
        isAsc: Boolean = dataManager.httpLogFilter.value.second
    ) {
        dataManager.httpLogFilter.value = input to isAsc
    }

    /** Searches for the input string in the body and headers and sorts the data set */
    fun filterLogs(
        input: String = dataManager.logFilter.value.first,
        isAsc: Boolean = dataManager.logFilter.value.second,
        level: Logger.Level? = dataManager.logFilter.value.third
    ) {
        dataManager.logFilter.value = Triple(input, isAsc, level)
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
        dataManager.streamingUrlResponse.value = BaseResponse.Idle
    }

    var remoteStreamDelay = SensorDelay.Normal
    fun setupRemoteStream(uri: CharSequence) {
        if(uri.isBlank()) return
        dataManager.streamingUrlResponse.value = BaseResponse.Loading
        viewModelScope.launch {
            dataManager.streamingUrlResponse.value = repository.postStreamData(
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
        activeExperimentScopes.values.forEach {
            it.cancel()
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

    private fun streamEvent(sensor: SensorEventListener, event: SensorEvent) {
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

            if(isLocalStreamRunning.value || dataManager.streamingUrlResponse.value is BaseResponse.Success) {
                streamChannel.trySend(newLine)
                streamLines.update {
                    it.toMutableList().apply {
                        add(0, newLine)
                    }
                }
            }
        }
    }

    private fun streamSetValues(
        experiment: ExperimentIO,
        values: List<ExperimentSetValue>,
        customValue: String?
    ) {
        val values = values.map { it.value }.let {
            json.encodeToJsonElement(it)
        }

        val jsonLine = buildJsonObject {
            put("experiment", experiment.name)
            put("set", experiment.name)
            customValue?.let { put("customValue", it) }
            put("values", values)
        }
        val newLine = json.encodeToString(jsonLine)

        if(isLocalStreamRunning.value || dataManager.streamingUrlResponse.value is BaseResponse.Success) {
            streamChannel.trySend(newLine)
            streamLines.update {
                it.toMutableList().apply {
                    add(0, newLine)
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
            dataManager.logs.value = listOf()
            dataManager.httpLogData.value = DeveloperUtils.HttpLogData()
            repository.clearAllDaos()
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