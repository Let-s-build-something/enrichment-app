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
    fun onAccuracyChanged(accuracy: Int)

    val id: Int
    val name: String
    val vendor: String
    val maximumRange: Float
    val resolution: Float
    var delay: SensorDelay

    fun register(sensorDelay: SensorDelay = SensorDelay.Normal)
    fun unregister()
    fun addListener(listener: (event: SensorEvent?) -> Unit) {
        this.listener = listener
    }

    val uid: String
        get() = "${name}_$id"
}