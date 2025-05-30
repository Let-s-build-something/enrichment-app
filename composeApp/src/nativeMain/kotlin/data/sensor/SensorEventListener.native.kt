package data.sensor

import augmy.interactive.shared.ext.ifNull
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.augmy.macos.getBatteryLevel
import org.augmy.macos.getForegroundApp
import org.augmy.macos.getMainDisplayBrightness
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceProximityStateDidChangeNotification
import platform.UIKit.UIScreen

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
    Proximity,
    BatteryLevel,
    ForegroundApp,
    ScreenBrightness
}


@OptIn(ExperimentalForeignApi::class)
private fun createListener(
    type: SensorType
): SensorEventListener {
    return object: SensorEventListener {
        override var data: MutableStateFlow<List<SensorEvent>> = MutableStateFlow(emptyList())
        override var listener: ((event: SensorEvent) -> Unit)? = null
        override val id: Int = type.ordinal
        override val name: String = type.name
        override val maximumRange: Float? = null
        override val resolution: Float? = null
        override var delay: SensorDelay = SensorDelay.Slow

        private val motionManager = CMMotionManager()
        private val altimeter = CMAltimeter()
        private val pedometer = CMPedometer()
        private val queue = NSOperationQueue.mainQueue
        private val device = UIDevice.currentDevice
        private var timerScope: CoroutineScope? = null

        override fun register(sensorDelay: SensorDelay) {
            val interval = sensorDelay.milliseconds / 1000.0
            delay = sensorDelay

            when (type) {
                SensorType.Accelerometer -> {
                    motionManager.accelerometerUpdateInterval = interval
                    motionManager.startAccelerometerUpdatesToQueue(queue) { data, _ ->
                        data?.let {
                            it.acceleration.useContents {
                                onSensorChanged(
                                    SensorEvent(
                                        timestamp = it.timestamp.toLong(),
                                        values = listOf(this.x.toFloat(), this.y.toFloat(), this.z.toFloat()).toFloatArray()
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
                                        timestamp = it.timestamp.toLong(),
                                        values = listOf(this.x.toFloat(), this.y.toFloat(), this.z.toFloat()).toFloatArray()
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
                                        timestamp = it.timestamp.toLong(),
                                        values = listOf(this.x.toFloat(), this.y.toFloat(), this.z.toFloat()).toFloatArray()
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
                                SensorEvent(timestamp = data.timestamp.toLong(), values = values)
                            )
                        }
                    }
                }

                SensorType.Pressure -> {
                    if (CMAltimeter.isRelativeAltitudeAvailable()) {
                        try {
                            altimeter.startRelativeAltitudeUpdatesToQueue(queue) { data, _ ->
                                data?.let {
                                    val pressure = it.pressure.doubleValue.toFloat()
                                    onSensorChanged(
                                        SensorEvent(values = listOf(pressure).toFloatArray())
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                SensorType.StepCounter -> {
                    if (CMPedometer.isStepCountingAvailable()) {
                        pedometer.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
                            data?.let {
                                onSensorChanged(
                                    SensorEvent(values = listOf(it.numberOfSteps.intValue.toFloat()).toFloatArray())
                                )
                            }
                        }
                    }
                }
                SensorType.ScreenBrightness -> {
                    registerManualCollector {
                        val brightness = getMainDisplayBrightness() ?: UIScreen.mainScreen.brightness.toFloat()
                        onSensorChanged(
                            SensorEvent(values = listOf(brightness).toFloatArray())
                        )
                    }
                }
                SensorType.ForegroundApp -> {
                    registerManualCollector {
                        getForegroundApp()?.let { appInfo ->
                            onSensorChanged(
                                SensorEvent(
                                    uiValues = mapOf(
                                        appInfo.localizedName to appInfo.bundleIdentifier
                                    )
                                )
                            )
                        }
                    }
                }
                SensorType.BatteryLevel -> {
                    device.setBatteryMonitoringEnabled(true)
                    getBatteryLevel()?.let { brightness ->
                        registerManualCollector {
                            onSensorChanged(
                                SensorEvent(values = listOf(brightness.toFloat()).toFloatArray())
                            )
                        }
                        brightness
                    }.ifNull {
                        NSNotificationCenter.defaultCenter.addObserverForName(
                            name = "UIDeviceBatteryLevelDidChangeNotification",
                            `object` = null,
                            queue = queue
                        ) { _ ->
                            onSensorChanged(
                                SensorEvent(values = listOf(UIDevice.currentDevice.batteryLevel).toFloatArray())
                            )
                        }
                    }
                }
                SensorType.Proximity -> {
                    device.setProximityMonitoringEnabled(true)
                    NSNotificationCenter.defaultCenter.addObserverForName(
                        name = UIDeviceProximityStateDidChangeNotification,
                        `object` = null,
                        queue = queue
                    ) { _ ->
                        val near = if (UIDevice.currentDevice.proximityState) 1f else 0f
                        onSensorChanged(
                            SensorEvent(values = listOf(near).toFloatArray())
                        )
                    }
                }
            }
        }

        override fun unregister() {
            when (type) {
                SensorType.Accelerometer -> motionManager.stopAccelerometerUpdates()
                SensorType.Gyroscope -> motionManager.stopGyroUpdates()
                SensorType.Magnetometer -> motionManager.stopMagnetometerUpdates()
                SensorType.Gravity,
                SensorType.LinearAcceleration,
                SensorType.RotationVector -> motionManager.stopDeviceMotionUpdates()
                SensorType.Pressure -> altimeter.stopRelativeAltitudeUpdates()
                SensorType.StepCounter -> pedometer.stopPedometerUpdates()
                SensorType.BatteryLevel -> device.setBatteryMonitoringEnabled(false)
                SensorType.Proximity -> device.setProximityMonitoringEnabled(false)
                else -> {}
            }

            timerScope?.coroutineContext?.cancelChildren()
            timerScope = null
        }

        private fun registerManualCollector(
            onCollection: () -> Unit
        ) {
            if (timerScope == null) {
                timerScope = CoroutineScope(Job())
            }else timerScope?.coroutineContext?.cancelChildren()

            timerScope?.launch {
                while (true) {
                    onCollection()
                    delay(delay.milliseconds)
                }
            }
        }
    }
}

actual suspend fun getAllSensors(): List<SensorEventListener>? = withContext(Dispatchers.Default) {
    val available = mutableListOf<SensorEventListener>()
    val motionManager = CMMotionManager()
    val altimeterAvailable = CMAltimeter.isRelativeAltitudeAvailable()
    val stepAvailable = CMPedometer.isStepCountingAvailable()
    val device = UIDevice.currentDevice

    available += createListener(SensorType.ScreenBrightness)
    available += createListener(SensorType.BatteryLevel)
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
    if (getForegroundApp() != null) {
        available += createListener(SensorType.ForegroundApp)
    }

    available
}
