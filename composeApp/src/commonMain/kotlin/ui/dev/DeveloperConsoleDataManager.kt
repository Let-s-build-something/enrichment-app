package ui.dev

import data.io.base.BaseResponse
import data.io.experiment.FullExperiment
import korlibs.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import utils.DeveloperUtils
import utils.SharedLogger.LoggerMessage

class DeveloperConsoleDataManager {

    /** developer console size */
    val developerConsoleSize = MutableStateFlow(0f)

    internal val logs = MutableStateFlow(listOf<LoggerMessage>())

    /** Log information for past or ongoing http calls */
    val httpLogData = MutableStateFlow(DeveloperUtils.HttpLogData())

    /** Current host override if there is any */
    val hostOverride = MutableStateFlow<String?>(null)

    /** filter input + whether it's ASC */
    val httpLogFilter = MutableStateFlow("" to false)

    val streamingUrlResponse = MutableStateFlow<BaseResponse<*>>(BaseResponse.Idle)

    /** filter input + whether it's ASC */
    val logFilter = MutableStateFlow<Triple<String, Boolean, Logger.Level?>>(Triple("", false, null))

    val experiments = MutableStateFlow(listOf<FullExperiment>())
    val activeExperiments = MutableStateFlow(setOf<String>())
    val experimentsToShow = MutableStateFlow(listOf<FullExperiment>())
}