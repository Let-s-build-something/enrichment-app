package data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_GAME
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.hardware.SensorManager.SENSOR_DELAY_UI
import org.koin.mp.KoinPlatform.getKoin

actual fun unregisterGravityListener(listener: SensorEventListener) {
    if(listener.isInitialized) {
        (listener.instance as? android.hardware.SensorEventListener)?.let { instance ->
            with(getKoin().get<Context>()) {
                (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(instance)
            }
        }
    }
}

actual fun registerGravityListener(
    listener: SensorEventListener,
    sensorDelay: SensorDelay
) {
    with(getKoin().get<Context>()) {
        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.apply {
            println("kostka_test, sensors: ${this.getSensorList(Sensor.TYPE_ALL).map { it.name }}")

            val eventListener = object: android.hardware.SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    listener.onSensorChanged(
                        event = SensorEvent(
                            accuracy = event?.accuracy,
                            timestamp = event?.timestamp,
                            values = event?.values
                        )
                    )
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    listener.onAccuracyChanged(accuracy = accuracy)
                    println("kostka_test, onAccuracyChanged: $accuracy, sensor: $sensor")
                }
            }
            listener.instance = eventListener
            listener.isInitialized = true
            registerListener(
                eventListener,
                getDefaultSensor(Sensor.TYPE_GRAVITY),
                when(sensorDelay) {
                    SensorDelay.Slow -> SENSOR_DELAY_NORMAL
                    SensorDelay.Normal -> SENSOR_DELAY_UI
                    SensorDelay.Fast -> SENSOR_DELAY_GAME
                }
            )
        }
    }
}