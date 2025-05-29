package data.sensor

import augmy.interactive.shared.utils.DateUtils
import korlibs.math.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import oshi.SystemInfo

actual suspend fun getAllSensors(): List<SensorEventListener>? {
    return with(SystemInfo()) {
        listOf(
            createRepeatedEventListener(name = "System uptime") {
                SensorEvent(
                    timestamp = DateUtils.now.toEpochMilliseconds(),
                    values = floatArrayOf(operatingSystem.systemUptime.toFloat())
                )
            },
            createRepeatedEventListener(name = "Battery capacity percentage") {
                SensorEvent(
                    timestamp = DateUtils.now.toEpochMilliseconds(),
                    values = floatArrayOf(
                        hardware.powerSources.firstOrNull()?.remainingCapacityPercent?.toFloat() ?: 0f)
                )
            },
            createRepeatedEventListener(name = "Battery charging bool state") {
                SensorEvent(
                    timestamp = DateUtils.now.toEpochMilliseconds(),
                    values = floatArrayOf(
                        hardware.powerSources.firstOrNull()?.isCharging?.toInt()?.toFloat() ?: 0f)
                )
            },
            createRepeatedEventListener(name = "Visible GUI windows") {
                SensorEvent(
                    timestamp = DateUtils.now.toEpochMilliseconds(),
                    values = null,
                    visibleWindowValues = operatingSystem.getDesktopWindows(true).map {
                        VisibleWindowValue(
                            name = it.title,
                            command = it.command
                        )
                    }
                )
            }
        )
    }
}

private fun createRepeatedEventListener(
    name: String,
    factory: () -> SensorEvent?
): SensorEventListener {
    return object : SensorEventListener {
        override var data: MutableStateFlow<List<SensorEvent>> = MutableStateFlow(emptyList())
        override val id: Int = this.hashCode()
        override val name: String = name
        override val maximumRange: Float? = null
        override val resolution: Float? = null
        override var delay: SensorDelay = SensorDelay.Slow
        private var runningScope = CoroutineScope(Job())

        override fun register(sensorDelay: SensorDelay) {
            unregister()
            runningScope.launch {
                while (true) {
                    onSensorChanged(factory())
                    delay(sensorDelay.milliseconds)
                }
            }
        }
        override fun unregister() {
            runningScope.coroutineContext.cancelChildren()
        }
    }
}

actual fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener? {
    return null
}
