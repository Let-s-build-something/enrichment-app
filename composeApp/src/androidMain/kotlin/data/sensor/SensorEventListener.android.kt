package data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_GAME
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.hardware.SensorManager.SENSOR_DELAY_UI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin

actual suspend fun getAllSensors(): List<SensorEventListener>? {
    return withContext(Dispatchers.Default) {
        getSensorManager()?.getSensorList(Sensor.TYPE_ALL)?.map { it.toSensorEventListener() }
    }
}

actual fun getGravityListener(onSensorChanged: (event: data.sensor.SensorEvent?) -> Unit): SensorEventListener? {
    return getSensorManager()?.getDefaultSensor(Sensor.TYPE_GRAVITY)?.toSensorEventListener()
}

private fun Sensor.toSensorEventListener(): SensorEventListener {
    return object: SensorEventListener {
        override lateinit var instance: Any
        override var isInitialized: Boolean = false
        override var listener: ((data.sensor.SensorEvent?) -> Unit)? = null

        override fun onSensorChanged(event: data.sensor.SensorEvent?) {
            listener?.invoke(event)
        }

        override fun onAccuracyChanged(accuracy: Int) {}

        override val id: Int = this@toSensorEventListener.id
        override val name: String = this@toSensorEventListener.name
        override val vendor: String = this@toSensorEventListener.vendor
        override val maximumRange: Float = this@toSensorEventListener.maximumRange
        override val resolution: Float = this@toSensorEventListener.resolution
        override var delay: SensorDelay = SensorDelay.Normal

        override fun register(sensorDelay: SensorDelay) {
            val eventListener = object: android.hardware.SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    onSensorChanged(event = event?.toSensorEvent())
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    onAccuracyChanged(accuracy = accuracy)
                    println("kostka_test, onAccuracyChanged: $accuracy, sensor: $sensor")
                }
            }
            delay = sensorDelay
            instance = eventListener
            isInitialized = true
            getSensorManager()?.registerListener(
                eventListener,
                this@toSensorEventListener,
                when(sensorDelay) {
                    SensorDelay.Slow -> SENSOR_DELAY_NORMAL
                    SensorDelay.Normal -> SENSOR_DELAY_UI
                    SensorDelay.Fast -> SENSOR_DELAY_GAME
                }
            )
        }

        override fun unregister() {
            if(isInitialized) {
                (instance as? android.hardware.SensorEventListener)?.let { instance ->
                    with(getKoin().get<Context>()) {
                        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(instance)
                    }
                }
            }
        }
    }
}

private fun getSensorManager() = getKoin()
    .get<Context>()
    .getSystemService(Context.SENSOR_SERVICE) as? SensorManager

private fun SensorEvent.toSensorEvent() = SensorEvent(
    accuracy = accuracy,
    timestamp = timestamp,
    values = values
)
