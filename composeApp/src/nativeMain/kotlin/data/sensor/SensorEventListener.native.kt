package data.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceProximityStateDidChangeNotification

actual fun getGravityListener(onSensorChanged: (event: SensorEvent?) -> Unit): SensorEventListener? {
    return null
}

enum class SensorType {
    Accelerometer,
    Gyroscope,
    Magnetometer,
    Gravity,
    LinearAcceleration,
    RotationVector,
    Pressure,
    StepCounter,
    Proximity
}


@OptIn(ExperimentalForeignApi::class)
private fun createListener(
    type: SensorType
): SensorEventListener {
    return object: SensorEventListener {
        override var instance: Any = ""
        override var isInitialized: Boolean = false
        override var listener: ((SensorEvent?) -> Unit)? = null
        override val id: Int = type.ordinal
        override val name: String = type.name
        override var delay: SensorDelay = SensorDelay.Normal

        private val motionManager = CMMotionManager()
        private val altimeter = CMAltimeter()
        private val pedometer = CMPedometer()
        private val queue = NSOperationQueue.mainQueue

        override fun register(sensorDelay: SensorDelay) {
            isInitialized = true
            val interval = when (sensorDelay) {
                SensorDelay.Slow -> 1.0
                SensorDelay.Normal -> 0.5
                SensorDelay.Fast -> 0.1
            }

            when (type) {
                SensorType.Accelerometer -> {
                    motionManager.accelerometerUpdateInterval = interval
                    motionManager.startAccelerometerUpdatesToQueue(queue) { data, _ ->
                        data?.let {
                            it.acceleration.useContents {
                                onSensorChanged(
                                    SensorEvent(
                                        it.timestamp.toLong(),
                                        listOf(this.x.toFloat(), this.y.toFloat(), this.z.toFloat()).toFloatArray()
                                    )
                                )
                            }
                        }
                    }
                }

                SensorType.Gyroscope -> {
                    motionManager.gyroUpdateInterval = interval
                    motionManager.startGyroUpdatesToQueue(queue) { data, _ ->
                        data?.let {
                            it.rotationRate.useContents {
                                onSensorChanged(
                                    SensorEvent(
                                        it.timestamp.toLong(),
                                        listOf(this.x.toFloat(), this.y.toFloat(), this.z.toFloat()).toFloatArray()
                                    )
                                )
                            }
                        }
                    }
                }

                SensorType.Magnetometer -> {
                    motionManager.magnetometerUpdateInterval = interval
                    motionManager.startMagnetometerUpdatesToQueue(queue) { data, _ ->
                        data?.let {
                            it.magneticField.useContents {
                                onSensorChanged(
                                    SensorEvent(
                                        it.timestamp.toLong(),
                                        listOf(this.x.toFloat(), this.y.toFloat(), this.z.toFloat()).toFloatArray()
                                    )
                                )
                            }
                        }
                    }
                }

                SensorType.Gravity,
                SensorType.LinearAcceleration,
                SensorType.RotationVector -> {
                    motionManager.deviceMotionUpdateInterval = interval
                    motionManager.startDeviceMotionUpdatesToQueue(queue) { updates, _ ->
                        updates?.let { data ->
                            val values = when (type) {
                                SensorType.Gravity -> {
                                    data.gravity.useContents {
                                        listOf(this.x, this.y, this.z)
                                    }
                                }
                                SensorType.LinearAcceleration -> {
                                    data.userAcceleration.useContents {
                                        listOf(this.x, this.y, this.z)
                                    }
                                }
                                SensorType.RotationVector -> {
                                    data.attitude.quaternion.useContents {
                                        listOf(this.x, this.y, this.z, this.w)
                                    }
                                }
                                else -> emptyList()
                            }.map { it.toFloat() }.toFloatArray()

                            onSensorChanged(
                                SensorEvent(data.timestamp.toLong(), values)
                            )
                        }
                    }
                }

                SensorType.Pressure -> {
                    if (CMAltimeter.isRelativeAltitudeAvailable()) {
                        altimeter.startRelativeAltitudeUpdatesToQueue(queue) { data, _ ->
                            data?.let {
                                val pressure = it.pressure.doubleValue.toFloat()
                                onSensorChanged(
                                    SensorEvent(
                                        NSDate().timeIntervalSince1970().toLong(),
                                        listOf(pressure).toFloatArray()
                                    )
                                )
                            }
                        }
                    }
                }

                SensorType.StepCounter -> {
                    if (CMPedometer.isStepCountingAvailable()) {
                        pedometer.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
                            data?.let {
                                onSensorChanged(
                                    SensorEvent(
                                        NSDate().timeIntervalSince1970().toLong(),
                                        listOf(it.numberOfSteps.intValue.toFloat()).toFloatArray()
                                    )
                                )
                            }
                        }
                    }
                }

                SensorType.Proximity -> {
                    UIDevice.currentDevice.proximityMonitoringEnabled = true
                    NSNotificationCenter.defaultCenter.addObserverForName(
                        name = UIDeviceProximityStateDidChangeNotification,
                        `object` = null,
                        queue = queue
                    ) { _ ->
                        val near = if (UIDevice.currentDevice.proximityState) 1f else 0f
                        onSensorChanged(
                            SensorEvent(
                                NSDate().timeIntervalSince1970().toLong(),
                                listOf(near).toFloatArray()
                            )
                        )
                    }
                }
            }
        }

        override fun unregister() {
            isInitialized = false
            when (type) {
                SensorType.Accelerometer -> motionManager.stopAccelerometerUpdates()
                SensorType.Gyroscope -> motionManager.stopGyroUpdates()
                SensorType.Magnetometer -> motionManager.stopMagnetometerUpdates()
                SensorType.Gravity,
                SensorType.LinearAcceleration,
                SensorType.RotationVector -> motionManager.stopDeviceMotionUpdates()
                SensorType.Pressure -> altimeter.stopRelativeAltitudeUpdates()
                SensorType.StepCounter -> pedometer.stopPedometerUpdates()
                SensorType.Proximity -> UIDevice.currentDevice.proximityMonitoringEnabled = false
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            listener?.invoke(event)
        }
    }
}

actual suspend fun getAllSensors(): List<SensorEventListener>? = withContext(Dispatchers.Default) {
    val available = mutableListOf<SensorEventListener>()
    val motionManager = CMMotionManager()
    val altimeterAvailable = CMAltimeter.isRelativeAltitudeAvailable()
    val stepAvailable = CMPedometer.isStepCountingAvailable()
    val device = UIDevice.currentDevice

    if (motionManager.isAccelerometerAvailable()) {
        available += createListener(SensorType.Accelerometer)
    }
    if (motionManager.isGyroAvailable()) {
        available += createListener(SensorType.Gyroscope)
    }
    if (motionManager.isMagnetometerAvailable()) {
        available += createListener(SensorType.Magnetometer)
    }
    if (motionManager.isDeviceMotionAvailable()) {
        available += createListener(SensorType.Gravity)
        available += createListener(SensorType.LinearAcceleration)
        available += createListener(SensorType.RotationVector)
    }
    if (altimeterAvailable) {
        available += createListener(SensorType.Pressure)
    }
    if (stepAvailable) {
        available += createListener(SensorType.StepCounter)
    }
    if (device.isProximityMonitoringEnabled()) {
        available += createListener(SensorType.Proximity)
    }

    available
}
