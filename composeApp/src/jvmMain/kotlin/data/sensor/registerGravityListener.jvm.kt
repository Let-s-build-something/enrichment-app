package data.sensor

import augmy.interactive.shared.utils.DateUtils
import korlibs.math.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import oshi.SystemInfo

actual suspend fun getAllSensors(): List<SensorEventListener>? {
    return with(SystemInfo()) {
        listOf(
            object : SensorEventListener {
                override var listener: ((SensorEvent?) -> Unit)? = null
                override val id: Int = this.hashCode()
                override val name: String = "System uptime"
                override val maximumRange: Float? = null
                override val resolution: Float? = null
                override var delay: SensorDelay = SensorDelay.Slow
                private var runningScope = CoroutineScope(Job())

                override fun register(sensorDelay: SensorDelay) {
                    unregister()
                    runningScope.launch {
                        while (true) {
                            onSensorChanged(SensorEvent(
                                timestamp = DateUtils.now.toEpochMilliseconds(),
                                values = floatArrayOf(operatingSystem.systemUptime.toFloat()))
                            )
                            delay(sensorDelay.milliseconds)
                        }
                    }
                }
                override fun unregister() {
                    runningScope.coroutineContext.cancelChildren()
                }
                override fun onSensorChanged(event: SensorEvent?) {
                    listener?.invoke(event)
                }
            },
            object : SensorEventListener {
                override var listener: ((SensorEvent?) -> Unit)? = null
                override val id: Int = this.hashCode()
                override val name: String = "Battery capacity percentage"
                override val maximumRange: Float? = null
                override val resolution: Float? = null
                override var delay: SensorDelay = SensorDelay.Slow
                private var runningScope = CoroutineScope(Job())

                override fun register(sensorDelay: SensorDelay) {
                    unregister()
                    runningScope.launch {
                        while (true) {
                            onSensorChanged(SensorEvent(
                                timestamp = DateUtils.now.toEpochMilliseconds(),
                                values = floatArrayOf(
                                    hardware.powerSources.firstOrNull()?.remainingCapacityPercent?.toFloat() ?: 0f)
                            )
                            )
                            delay(sensorDelay.milliseconds)
                        }
                    }
                }
                override fun unregister() {
                    runningScope.coroutineContext.cancelChildren()
                }
                override fun onSensorChanged(event: SensorEvent?) {
                    listener?.invoke(event)
                }
            },
            object : SensorEventListener {
                override var listener: ((SensorEvent?) -> Unit)? = null
                override val id: Int = this.hashCode()
                override val name: String = "Battery charging bool state"
                override val maximumRange: Float? = null
                override val resolution: Float? = null
                override var delay: SensorDelay = SensorDelay.Slow
                private var runningScope = CoroutineScope(Job())

                override fun register(sensorDelay: SensorDelay) {
                    unregister()
                    runningScope.launch {
                        while (true) {
                            onSensorChanged(
                                SensorEvent(
                                    timestamp = DateUtils.now.toEpochMilliseconds(),
                                    values = floatArrayOf(
                                        hardware.powerSources.firstOrNull()?.isCharging?.toInt()?.toFloat() ?: 0f)
                                )
                            )
                            delay(sensorDelay.milliseconds)
                        }
                    }
                }
                override fun unregister() {
                    runningScope.coroutineContext.cancelChildren()
                }
                override fun onSensorChanged(event: SensorEvent?) {
                    listener?.invoke(event)
                }
            },
            object : SensorEventListener {
                override var listener: ((SensorEvent?) -> Unit)? = null
                override val id: Int = this.hashCode()
                override val name: String = "Visible GUI windows"
                override val maximumRange: Float? = null
                override val resolution: Float? = null
                override var delay: SensorDelay = SensorDelay.Slow
                private var runningScope = CoroutineScope(Job())

                override fun register(sensorDelay: SensorDelay) {
                    unregister()
                    runningScope.launch {
                        while (true) {
                            onSensorChanged(SensorEvent(
                                timestamp = DateUtils.now.toEpochMilliseconds(),
                                values = null,
                                visibleWindowValues = operatingSystem.getDesktopWindows(true).map {
                                    VisibleWindowValue(
                                        name = it.title,
                                        command = it.command
                                    )
                                }
                            ))
                            delay(sensorDelay.milliseconds)
                        }
                    }
                }
                override fun unregister() {
                    runningScope.coroutineContext.cancelChildren()
                }
                override fun onSensorChanged(event: SensorEvent?) {
                    listener?.invoke(event)
                }
            },
        )
    }
}

actual fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener? {
    return null
}
