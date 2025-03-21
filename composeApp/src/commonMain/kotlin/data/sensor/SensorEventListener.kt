package data.sensor


expect fun registerGravityListener(listener: SensorEventListener)

interface SensorEventListener {
    fun onSensorChanged(event: SensorEvent?)
    fun onAccuracyChanged(accuracy: Int)
}