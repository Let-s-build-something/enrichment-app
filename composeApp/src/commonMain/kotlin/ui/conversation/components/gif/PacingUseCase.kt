package ui.conversation.components.gif

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.dsl.module

internal val pacingModule = module {
    factory { PacingUseCase() }
}

/** Bundled functionality of Gifs */
class PacingUseCase {
    companion object {
        private const val WIDE_AVG_WEIGHT = 0.33f
        private const val NARROW_AVG_WEIGHT = 0.33f
        private const val CLOSE_AVG_WEIGHT = 0.33f

        private const val MIN_WAVES = 5
    }

    private val mutex = Mutex()
    private val keyboardRows = listOf(
        "1234567890",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )
    private val _keyWidths = MutableStateFlow(MutableList(MIN_WAVES) { 0f })
    private var backStack = MutableList(MIN_WAVES) { it to 0f }
    val keyWidths = _keyWidths.asStateFlow()

    fun init(maxWaves: Int) {
        _keyWidths.update {
            MutableList(maxWaves) { 0f }
        }
        backStack = MutableList(maxWaves) { it to 0f }
    }

    // 1. define number of waves for the message
    suspend fun onKeyPressed(char: Char, timingMs: Long) = withContext(Dispatchers.Default) {
        mutex.withLock {
            while(backStack.size > keyWidths.value.size) {
                backStack.removeFirst()
            }
            val positionScale = getKeyScalePosition(char)
            val position = (keyWidths.value.size * positionScale).toInt().let { position ->
                // we look for closest least active index
                var leastActiveIndex: Pair<Int, Float>? = null
                val indexes = keyWidths.value.size - position
                for(i in 1..(indexes)) {
                    val scale = (1f - i.toFloat() / indexes.toFloat()) / 2

                    backStack.getOrNull(position + i)?.let {
                        if(it.second < (leastActiveIndex?.second?.times(scale) ?: 0f)) leastActiveIndex = it
                    }
                    backStack.getOrNull(position - i)?.let {
                        if(it.second < (leastActiveIndex?.second?.times(scale) ?: 0f)) leastActiveIndex = it
                    }
                }
                (if(leastActiveIndex == null) {
                    backStack.removeFirstOrNull()?.first
                }else leastActiveIndex?.first) ?: position
            }.coerceAtMost(keyWidths.value.lastIndex)

            // weights
            val wideAvg = 500f // TODO
            val narrowAvg = 800f // TODO
            val closeAvg = 1000f // TODO

            val wideValance = (if(timingMs < wideAvg) {
                1 - timingMs / wideAvg
            }else 0f).times(WIDE_AVG_WEIGHT)

            val narrowValance = (if(timingMs < narrowAvg) {
                1 - timingMs / narrowAvg
            }else 0f).times(NARROW_AVG_WEIGHT)

            val closeValance = (if(timingMs < closeAvg) {
                1 - timingMs / closeAvg
            }else 0f).times(CLOSE_AVG_WEIGHT)


            val valance = (wideValance + narrowValance + closeValance).coerceIn(minimumValue = 0f , maximumValue = 1f)
            println("kostka_test, valance: $valance ($wideValance + $narrowValance + $closeValance), timing: $timingMs")
            _keyWidths.update {
                it.apply {
                    this[position] = valance
                    // previous valances fade out based on percentage of how far they are
                    backStack.forEachIndexed { index, previousValance ->
                        this[previousValance.first] = previousValance.second * index.plus(1f) / backStack.size.plus(2f)
                    }
                }
            }
            backStack.add(position to valance)
        }
    }

    private suspend fun getKeyScalePosition(key: Char): Double = withContext(Dispatchers.Default) {
        for (row in keyboardRows) {
            val index = row.lastIndexOf(key.lowercaseChar())
            println("kostka_test, getKeyScalePosition, index: $index")
            if (index != -1) {
                return@withContext index.toDouble() / (row.length - 1).toDouble()
            }
        }
        0.5
    }
}
