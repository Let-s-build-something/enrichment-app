package data.sensor


enum class SensorDelay {
    Slow,
    Normal,
    Fast
}

expect fun registerGravityListener(listener: SensorEventListener, sensorDelay: SensorDelay)
expect fun unregisterGravityListener(listener: SensorEventListener)

interface SensorEventListener {
    var instance: Any
    fun onSensorChanged(event: SensorEvent?)
    fun onAccuracyChanged(accuracy: Int)
}