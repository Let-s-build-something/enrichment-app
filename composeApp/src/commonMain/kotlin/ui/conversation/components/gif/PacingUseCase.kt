package ui.conversation.components.gif

import data.io.app.SettingsKeys.KEY_CLOSE_WIDE_AVG
import data.io.app.SettingsKeys.KEY_NARROW_WIDE_AVG
import data.io.app.SettingsKeys.KEY_PACING_WIDE_AVG
import koin.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.dsl.module

internal val pacingModule = module {
    factory {
        PacingUseCase(get())
    }
}

/** Bundled functionality of Gifs */
class PacingUseCase(
    private val settings: AppSettings
) {
    companion object {
        private const val WIDE_AVG_WEIGHT = 0.33f
        private const val NARROW_AVG_WEIGHT = 0.33f
        private const val CLOSE_AVG_WEIGHT = 0.33f

        private const val MIN_WAVES = 8
    }

    private val mutex = Mutex()
    private val keyboardRows = listOf(
        "1234567890",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )
    private val _keyWidths = MutableStateFlow(MutableList(MIN_WAVES) { 0.1f })
    private var backStack = MutableList(MIN_WAVES) { it to 0.1f }
    val keyWidths = _keyWidths.asStateFlow()

    private var wideAvg = 300f
    private var narrowAvg = 300f
    private var closeAvg = 300f

    suspend fun init(maxWaves: Int) {
        wideAvg = settings.getFloatOrNull(KEY_PACING_WIDE_AVG) ?: 300f
        narrowAvg = //TODO from X last messages
        closeAvg = //TODO current message only

        _keyWidths.update {
            MutableList(maxWaves.coerceAtLeast(MIN_WAVES)) { 0.1f }
        }
        backStack = MutableList(maxWaves.coerceAtLeast(MIN_WAVES)) { it to 0.1f }
    }

    private val scope = CoroutineScope(Job())
    private fun runDecreaseRepeat() {
        scope.coroutineContext.cancelChildren()
        scope.launch {
            while (true) {
                delay(200)
                _keyWidths.update { prev ->
                    prev.map { it.times(0.95f).coerceAtLeast(0.1f) }.toMutableList().also {
                        backStack = it.mapIndexed { index, value -> index to value }.toMutableList()
                    }
                }
            }
        }
    }

    // 1. define number of waves for the message
    suspend fun onKeyPressed(char: Char, timingMs: Long) = withContext(Dispatchers.Default) {
        mutex.withLock {
            while(backStack.size > keyWidths.value.size) {
                backStack.removeFirst()
            }
            val positionScale = getKeyScalePosition(char)
            val position = (keyWidths.value.size * positionScale).toInt().let {
                // we don't want to override previously selected index right away
                if(backStack.lastOrNull()?.first == it) {
                    it + (if(positionScale >= 0.5) -1 else 1)
                }else it
            }.coerceAtMost(keyWidths.value.lastIndex)

            val wideValance = (if(timingMs < wideAvg) {
                1 - timingMs / wideAvg
            }else 0f).times(WIDE_AVG_WEIGHT)

            val narrowValance = (if(timingMs < narrowAvg) {
                1 - timingMs / narrowAvg
            }else 0f).times(NARROW_AVG_WEIGHT)

            val closeValance = (if(timingMs < closeAvg) {
                1 - timingMs / closeAvg
            }else 0f).times(CLOSE_AVG_WEIGHT)


            val valance = (wideValance + narrowValance + closeValance)
                .times(1.25f)
                .coerceIn(minimumValue = 0f, maximumValue = 1f)

            _keyWidths.update {
                it.apply {
                    this[position] = valance
                }
            }
            backStack.add(position to valance)
            runDecreaseRepeat()
        }
    }

    private suspend fun getKeyScalePosition(key: Char): Double = withContext(Dispatchers.Default) {
        for (row in keyboardRows) {
            val index = row.lastIndexOf(key.lowercaseChar())
            if (index != -1) {
                return@withContext index.toDouble() / (row.length - 1).toDouble()
            }
        }
        0.5
    }
}
