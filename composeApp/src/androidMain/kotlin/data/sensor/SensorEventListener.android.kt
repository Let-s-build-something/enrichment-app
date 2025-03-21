package data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_UI
import org.koin.mp.KoinPlatform.getKoin

actual fun registerGravityListener(listener: SensorEventListener) {
    with(getKoin().get<Context>()) {
        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.apply {
            println("kostka_test, sensors: ${this.getSensorList(Sensor.TYPE_ALL).map { it.name }}")
            registerListener(
                object: android.hardware.SensorEventListener {
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
                },
                getDefaultSensor(Sensor.TYPE_GRAVITY) as Sensor,
                SENSOR_DELAY_UI
            )
        }
    }
}