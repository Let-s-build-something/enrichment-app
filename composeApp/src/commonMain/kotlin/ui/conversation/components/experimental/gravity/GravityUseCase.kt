package ui.conversation.components.experimental.gravity

import augmy.interactive.shared.ui.base.DeviceOrientation
import augmy.interactive.shared.utils.DateUtils
import data.io.app.SettingsKeys
import data.sensor.SensorDelay
import data.sensor.SensorEvent
import data.sensor.SensorEventListener
import data.sensor.registerGravityListener
import data.sensor.unregisterGravityListener
import koin.AppSettings
import korlibs.math.roundDecimalPlaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.math.absoluteValue

internal val gravityModule = module {
    factory { GravityUseCase() }
}

@Serializable
data class GravityData(
    val values: List<GravityValue>,
    val tickMs: Long
)

@Serializable
data class GravityValue(
    val fraction: Float,
    val offset: Float
)

/** Bundled functionality of Gifs */
class GravityUseCase {
    companion object {
        const val FULL_GRAVITY = 9.8f
        const val MAX_FRACTION_OFFSET = .25f
        const val TICK_MILLIS = 200L
    }

    private val settings: AppSettings by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()

    private val gravityValuesCache = mutableListOf<Pair<GravityValue, Long>>()
    private val sensorMutex = Mutex()
    private val sensorScope = CoroutineScope(Job())
    var deviceOrientation = DeviceOrientation.Vertical

    private var listener = object: SensorEventListener {
        override lateinit var instance: Any

        override fun onSensorChanged(event: SensorEvent?) {
            sensorScope.launch {
                sensorMutex.withLock {
                    event?.values?.let { values ->
                        gravityValuesCache.add(
                            calculateIndicationStops(
                                isVertical = deviceOrientation == DeviceOrientation.Vertical,
                                gx = values[0].roundDecimalPlaces(3),
                                gy = values[1].roundDecimalPlaces(3),
                                gz = values[2].roundDecimalPlaces(3)
                            ) to DateUtils.now.toEpochMilliseconds().also {
                                println("kostka_test, GravityUseCase, time it took from last report: " +
                                        "${it - (gravityValuesCache.lastOrNull()?.second ?: it)}")
                            }
                        )
                    }
                }
            }
        }
        override fun onAccuracyChanged(accuracy: Int) {

        }
    }

    init {
        registerGravityListener(listener, sensorDelay = SensorDelay.Slow)
    }


    // =============== public functions ===============

    fun kill() {
        unregisterGravityListener(listener)
    }

    fun onTick() {
        println("kostka_test, onTick")
        //TODO
    }

    // TODO sqlite, data is heavy
    suspend fun save(conversationId: String) = withContext(Dispatchers.Default) {
        val key = "${SettingsKeys.KEY_LAST_MESSAGE_GRAVITY}_$conversationId"

        if(gravityValuesCache.isNotEmpty()) {
            settings.putString(key, json.encodeToString(gravityValuesCache))
        }else settings.remove(key)
    }


    // ================ util functions ================

    private suspend fun calculateIndicationStops(
        isVertical: Boolean,
        gx: Float,
        gy: Float,
        gz: Float
    ): GravityValue = withContext(Dispatchers.Default) {
        val x = if(isVertical) gx else gy
        val y = if(isVertical) gy else gx

        val verticalFraction = (y.absoluteValue / FULL_GRAVITY).minus(1f).absoluteValue * MAX_FRACTION_OFFSET
        val value = (x / FULL_GRAVITY / 2 + .5f).minus(1).absoluteValue.coerceAtMost(1f)

        GravityValue(
            fraction = value,
            offset = verticalFraction / 2
        )
    }
}
