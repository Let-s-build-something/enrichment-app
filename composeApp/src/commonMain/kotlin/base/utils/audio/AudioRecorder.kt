package base.utils.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Audio recorder for recording a byteArray */
abstract class AudioRecorder(
    private val sampleRate: Int,
    secondsPerBar: Double,
    barsCount: Int
): AudioProcessor(
    sampleRate = sampleRate,
    secondsPerBar = secondsPerBar,
    barsCount = barsCount
) {
    abstract fun startRecording()
    abstract suspend fun saveRecording(): ByteArray?
    abstract fun pauseRecording()

    open fun stopRecording() {
        barPeakAmplitudes.value = listOf()
        samplePeakSum = 0.0
        samplePeakCount = 0
        peakMedian.doubleValue = 0.0
    }

    protected suspend fun pcmToWav(
        pcmData: ByteArray?,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray? {
        if(pcmData == null) return null
        return withContext(Dispatchers.Default) {
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = channels * bitsPerSample / 8
            val dataSize = pcmData.size
            val fileSize = 36 + dataSize

            ByteArray(44).apply {
                writeString(0, "RIFF")                   // ChunkID
                writeIntLE(4, fileSize)                  // ChunkSize
                writeString(8, "WAVE")                   // Format

                // fmt Subchunk
                writeString(12, "fmt ")                  // Subchunk1ID
                writeIntLE(16, 16)                       // Subchunk1Size (PCM Header Size)
                writeShortLE(20, 1)                      // AudioFormat (1 = PCM)
                writeShortLE(22, channels.toShort())     // NumChannels
                writeIntLE(24, sampleRate)               // SampleRate
                writeIntLE(28, byteRate)                 // ByteRate
                writeShortLE(32, blockAlign.toShort())   // BlockAlign
                writeShortLE(34, bitsPerSample.toShort())// BitsPerSample

                // data Subchunk
                writeString(36, "data")                  // Subchunk2ID
                writeIntLE(40, dataSize)              // Subchunk2Size
            } + pcmData
        }
    }

    // Helper Functions
    private fun ByteArray.writeString(offset: Int, value: String) {
        value.toByteArray().copyInto(this, offset)
    }

    private fun ByteArray.writeIntLE(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
        this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun ByteArray.writeShortLE(offset: Int, value: Short) {
        this[offset] = (value.toInt() and 0xFF).toByte()
        this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
}

/** Class for processing audio. Both for recording and playing. */
abstract class AudioProcessor(
    sampleRate: Int,
    secondsPerBar: Double,
    val barsCount: Int
) {
    protected val bytesPerBar = (sampleRate * secondsPerBar * 2).toInt()
    protected var samplePeakSum = 0.0
    protected var samplePeakCount = 0

    val barPeakAmplitudes = mutableStateOf(listOf<Pair<Double, String>>())
    val peakMedian = mutableDoubleStateOf(0.0)

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
    bufferSize: Int = sampleRate / 11
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
