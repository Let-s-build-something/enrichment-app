package ui.conversation.components.experimental.gravity

import data.sensor.getGravityListener
import database.dao.GravityDao
import korlibs.math.roundDecimalPlaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.math.absoluteValue

internal val gravityModule = module {
    factory { GravityUseCase() }
}

/** Bundled functionality of Gifs */
class GravityUseCase {
    companion object {
        const val FULL_GRAVITY = 9.8f
        const val MAX_FRACTION_OFFSET = .25f
        const val TICK_MILLIS = 100L
    }
    private val gravityDao: GravityDao by KoinPlatform.getKoin().inject()

    private var conversationId = ""
    private val gravityValuesCache = mutableListOf<GravityValue>()
    private val sensorMutex = Mutex()
    private val sensorScope = CoroutineScope(Job())
    val gravityValues = MutableStateFlow(listOf<GravityValue>())

    private var listener = getGravityListener { event ->
        sensorScope.launch {
            sensorMutex.withLock {
                event?.values?.let { values ->
                    gravityValuesCache.add(
                        calculateIndicationStops(
                            gx = values[0].roundDecimalPlaces(3),
                            gy = values[1].roundDecimalPlaces(3),
                            //gz = values[2].roundDecimalPlaces(3)
                        )
                    )
                }
            }
        }
    }

    // =============== public functions ===============

    suspend fun init(conversationId: String) {
        this.conversationId = conversationId
        gravityValues.value = gravityDao.getAll(conversationId)
    }

    fun start() {
        listener?.register()
    }

    fun kill() {
        listener?.unregister()
    }

    suspend fun onTick() {
        gravityValuesCache.lastOrNull()?.let { value ->
            gravityDao.insert(value)
            gravityValues.update {
                it + value
            }
        }
    }

    suspend fun clearCache(conversationId: String) = withContext(Dispatchers.Default) {
        gravityDao.removeAll(conversationId)
    }


    // ================ util functions ================

    private suspend fun calculateIndicationStops(
        gx: Float,
        gy: Float
    ): GravityValue = withContext(Dispatchers.Default) {
        val verticalFraction = (gy.absoluteValue / FULL_GRAVITY).minus(1f).absoluteValue * MAX_FRACTION_OFFSET
        val value = (gx / FULL_GRAVITY / 2 + .5f).minus(1).absoluteValue.coerceAtMost(1f)

        GravityValue(
            fraction = value,
            offset = verticalFraction / 2,
            conversationId = conversationId
        )
    }
}
