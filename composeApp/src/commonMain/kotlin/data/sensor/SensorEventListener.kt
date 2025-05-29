package data.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


enum class SensorDelay {
    Slow,
    Normal,
    Fast;

    val milliseconds: Long
        get() = when (this) {
            Slow -> 3_000
            Normal -> 1_000
            Fast -> 100
        }
}

expect fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener?
expect suspend fun getAllSensors(): List<SensorEventListener>?

interface SensorEventListener {
    var data: MutableStateFlow<List<SensorEvent>>

    fun onSensorChanged(event: SensorEvent?) {
        if (event != null) data.update {
            it.toMutableList().apply {
                add(0, event)
            }
        }
    }

    val id: Int
    val name: String
    val maximumRange: Float?
    val resolution: Float?
    var delay: SensorDelay
    val uid: String
        get() = "$id-$name"

    fun register(sensorDelay: SensorDelay = SensorDelay.Slow)
    fun unregister()
}
