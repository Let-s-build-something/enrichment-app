package data.sensor


enum class SensorDelay {
    Slow,
    Normal,
    Fast;

    val milliseconds: Long
        get() = when (this) {
            Slow -> 1000
            Normal -> 500
            Fast -> 100
        }
}

expect fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener?
expect suspend fun getAllSensors(): List<SensorEventListener>?

interface SensorEventListener {
    var listener: ((event: SensorEvent?) -> Unit)?
    fun onSensorChanged(event: SensorEvent?)

    val id: Int
    val name: String
    val maximumRange: Float?
    val resolution: Float?
    var delay: SensorDelay
    val uid: String
        get() = "$id-$name"

    fun register(sensorDelay: SensorDelay = SensorDelay.Normal)
    fun unregister()
}
