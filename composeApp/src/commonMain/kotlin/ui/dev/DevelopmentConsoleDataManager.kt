package ui.dev

import korlibs.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import utils.DeveloperUtils
import utils.SharedLogger.LoggerMessage

class DevelopmentConsoleDataManager {

    /** developer console size */
    val developerConsoleSize = MutableStateFlow(0f)

    internal val logs = MutableStateFlow(listOf<LoggerMessage>())

    /** Log information for past or ongoing http calls */
    val httpLogData = MutableStateFlow(DeveloperUtils.HttpLogData())

    /** Current host override if there is any */
    val hostOverride = MutableStateFlow<String?>(null)

    /** filter input + whether it's ASC */
    val httpLogFilter = MutableStateFlow<Pair<String, Boolean>>("" to false)

    /** filter input + whether it's ASC */
    val logFilter = MutableStateFlow<Triple<String, Boolean, Logger.Level?>>(Triple("", false, null))
}