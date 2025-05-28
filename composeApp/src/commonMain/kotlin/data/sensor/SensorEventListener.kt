package data.sensor


enum class SensorDelay {
    Slow,
    Normal,
    Fast
}

expect fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener?
expect suspend fun getAllSensors(): List<SensorEventListener>?

interface SensorEventListener {
    var instance: Any
    var isInitialized: Boolean
    var listener: ((event: SensorEvent?) -> Unit)?
    fun onSensorChanged(event: SensorEvent?)

    val id: Int
    val name: String
    var delay: SensorDelay

    fun register(sensorDelay: SensorDelay = SensorDelay.Normal)
    fun unregister()
}
