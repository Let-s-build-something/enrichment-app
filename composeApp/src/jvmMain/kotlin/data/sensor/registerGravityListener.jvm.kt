package data.sensor

actual suspend fun getAllSensors(): List<SensorEventListener>? {
    return null
}

actual fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener? {
    return null
}
