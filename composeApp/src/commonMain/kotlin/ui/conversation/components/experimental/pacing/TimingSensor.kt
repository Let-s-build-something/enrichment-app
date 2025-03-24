package ui.conversation.components.experimental.pacing

import augmy.interactive.shared.utils.StopwatchCounter
import ui.conversation.components.REGEX_GRAPHEME

/** Each keystroke has */
class TimingSensor(
    val isLocked: Boolean = false,
    val timings: MutableList<Long> = mutableListOf(),
    initialText: CharSequence = ""
) {
    private val counter = StopwatchCounter(tickMillis = 10)
    private var previousValue = initialText
    var isRunning: Boolean = false

    /** Flushes all variables */
    fun flush() {
        counter.flush()
        previousValue = ""
        timings.clear()
    }

    /** Pauses the counter */
    fun pause() {
        counter.stop()
        isRunning = false
    }

    fun start() {
        if(isLocked) return
        counter.start()
        isRunning = true
    }

    fun onTick(ms: Long, callback: () -> Unit) { counter.onTick(ms, callback) }

    data class NewTextDifference(
        val newChar: Char,
        val timing: Long
    )

    /**
     * Sets the timing of newly added grapheme
     * Must be called on each key stroke to update timings.
     */
    fun onNewText(value: CharSequence): NewTextDifference? {
        if(isLocked) return null

        val newLength = REGEX_GRAPHEME.toRegex().findAll(value).toList().size
        val previousLength = REGEX_GRAPHEME.toRegex().findAll(previousValue).toList().size

        previousValue = value
        if(newLength > previousLength) {
            val timing = counter.reset()
            isRunning = true

            val length = newLength - previousLength
            // copy pasting
            if(length > 1) {
                repeat(length - 1) {
                    timings.add(0L)
                }
            }else {
                timings.add(timing)
                return NewTextDifference(
                    newChar = value.removePrefix(previousValue).lastOrNull() ?: value.lastOrNull() ?: ' ',
                    timing = timing
                )
            }
        }else if (newLength < previousLength) {
            for(i in 0 until previousLength - newLength) {
                timings.removeLastOrNull()
            }
        }
        return null
    }
}