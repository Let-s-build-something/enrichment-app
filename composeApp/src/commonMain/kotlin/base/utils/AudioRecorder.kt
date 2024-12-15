package base.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

abstract class AudioRecorder(
    sampleRate: Int,
    secondsPerBar: Double,
    private val barsCount: Int,
) {
    protected val bytesPerBar = (sampleRate * secondsPerBar * 2).toInt()
    private var samplePeakSum = 0.0

    private var samplePeakCount = 0
    val barPeakAmplitudes = mutableStateOf(listOf<Pair<Double, String>>())
    val peakMedian = mutableDoubleStateOf(0.0)

    abstract fun startRecording()
    abstract suspend fun saveRecording(): ByteArray?
    abstract fun pauseRecording()

    open fun stopRecording() {
        barPeakAmplitudes.value = listOf()
        samplePeakSum = 0.0
        samplePeakCount = 0
        peakMedian.doubleValue = 0.0
    }

    @OptIn(ExperimentalUuidApi::class)
    protected suspend fun processChunk(byteArray: ByteArray?) {
        withContext(Dispatchers.Default) {
            generateChunkedPeaks(
                byteArray = byteArray,
                samplesPerBar = bytesPerBar / 2,
            )?.let { peaks ->
                val amplitudes = peaks.map { d -> d to Uuid.random().toString() }
                samplePeakSum += peaks.sum()
                samplePeakCount += peaks.size
                peakMedian.doubleValue = samplePeakSum / samplePeakCount * 3.5f
                barPeakAmplitudes.value = barPeakAmplitudes.value.plus(amplitudes).takeLast(barsCount)
            }
        }
    }
}

/** Automatically disposed audio recorder */
@Composable
expect fun rememberAudioRecorder(
    barsCount: Int,
    sampleRate: Int = 44100,
    secondsPerBar: Double = 0.25,
    bufferSize: Int = sampleRate / 21
): AudioRecorder

/**
 * Generates bars for a waveform out of raw PCM audio [byteArray]
 */
private fun generateChunkedPeaks(
    byteArray: ByteArray?,
    samplesPerBar: Int
): List<Double>? {
    if (byteArray == null || byteArray.isEmpty()) return null

    val samples = extractAmplitudes(byteArray)

    if (samplesPerBar > samples.size) {
        return null
    }

    val peaks = mutableListOf<Double>()

    /*for (i in samples.indices step samplesPerBar) {
        val chunk = samples.subList(i, minOf(i + samplesPerBar, samples.size))
        peaks.add(chunk.maxOfOrNull { it.absoluteValue } ?: 0.0)
    }*/

    for (i in samples.indices step samplesPerBar) {
        val chunk = samples.subList(i, minOf(i + samplesPerBar, samples.size))

        // Calculate the RMS value for the chunk
        val sumOfSquares = chunk.sumOf { it * it }
        val rms = kotlin.math.sqrt(sumOfSquares / chunk.size)
        peaks.add(rms)
    }
    return peaks
}

private fun extractAmplitudes(byteArray: ByteArray?): List<Double> {
    if(byteArray == null) return emptyList()

    val amplitudes = mutableListOf<Double>()
    for (i in byteArray.indices step 2) {
        if (i + 1 < byteArray.size) {
            val amplitude = (byteArray[i + 1].toInt().absoluteValue shl 8 or (byteArray[i].toInt().absoluteValue and 0xFF)).toDouble()
            amplitudes.add(amplitude / Short.MAX_VALUE)
        }
    }
    return amplitudes
}
