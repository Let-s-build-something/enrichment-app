package augmy.interactive.shared.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Counter that counts time in milliseconds
 * @param tickMillis time in milliseconds between each tick
 */
class StopwatchCounter(
    private val tickMillis: Long = 100L
) {
    private val scope = CoroutineScope(Job())
    private val listeners = mutableListOf<Pair<Long, () -> Unit>>()

    private var millis: Long = 0L

    @Throws(IllegalArgumentException::class)
    fun onTick(ms: Long, callback: () -> Unit) {
        if(ms % tickMillis != 0L) {
            throw IllegalArgumentException("$ms must be dividable by $tickMillis")
        }
        listeners.remove(ms to callback)
        listeners.add(ms to callback)
    }

    /** stops the counter */
    fun stop() {
        scope.coroutineContext.cancelChildren()
    }

    /** stops and clears the counter */
    fun flush(): Long {
        stop()
        val oldValue = millis.div(1)
        millis = 0
        return oldValue
    }

    /** starts the counter */
    fun start() {
        stop()
        scope.launch {
            while (true) {
                delay(tickMillis)
                millis += tickMillis
                listeners.forEach { listener ->
                    if(millis % listener.first == 0L) {
                        listener.second.invoke()
                    }
                }
            }
        }
    }

    /** resets the counter and returns current value */
    fun reset(): Long {
        val oldValue = millis.div(1)
        millis = 0
        start()
        return oldValue
    }
}