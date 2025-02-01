package ui.conversation.components

import augmy.interactive.shared.utils.StopwatchCounter

/** Each keystroke has */
class TimingSensor(
    val timings: MutableList<Long> = mutableListOf(),
    initialText: CharSequence = ""
) {
    private val counter = StopwatchCounter(tickMillis = 10)
    private var previousValue = initialText

    /** Flushes all variables */
    fun flush() {
        counter.flush()
        previousValue = ""
        timings.clear()
    }

    /** Pauses the counter */
    fun pause() {
        counter.stop()
    }

    /**
     * Sets the timing of newly added grapheme
     * Must be called on each key stroke to update timings.
     */
    fun onNewText(value: CharSequence) {
        val newLength = REGEX_GRAPHEME.toRegex().findAll(value).toList().size
        val previousLength = REGEX_GRAPHEME.toRegex().findAll(previousValue).toList().size

        if(newLength > previousLength) {
            val timing = counter.reset()

            val length = newLength - previousLength
            timings.add(timing)
            // copy pasting
            if(length > 1) {
                repeat(length - 1) {
                    timings.add(0L)
                }
            }
        }else if (newLength < previousLength) {
            for(i in 0 until previousLength - newLength) {
                timings.removeLastOrNull()
            }
        }
        previousValue = value
    }
}