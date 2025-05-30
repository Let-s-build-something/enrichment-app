package base.utils.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Automatically disposed audio player
 * @param secondsPerBar how many seconds should be represented by a single bar
 */
@Composable
actual fun rememberAudioPlayer(
    byteArray: ByteArray,
    onFinish: () -> Unit,
    barsCount: Int,
    sampleRate: Int,
    secondsPerBar: Double,
    bufferSize: Int
): AudioPlayer {
    return remember(byteArray.size) {
        val scope = CoroutineScope(Job() + Dispatchers.IO)

        object: AudioPlayer(
            byteArray = byteArray,
            barsCount = barsCount,
            secondsPerBar = secondsPerBar,
            bufferSize = bufferSize,
            scope = scope
        ) {
            private val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            private var line: SourceDataLine? = null
            private var offset = 0
            private var playedBytes = 0

            // bar creation
            private var barBuffer: ByteArrayOutputStream? = null

            override fun play() {
                scope.launch {
                    if(line == null || line?.isOpen == false) {
                        line = AudioSystem.getSourceDataLine(format)
                        barBuffer = ByteArrayOutputStream()
                        line?.open(format)
                        offset = 0
                        playedBytes = 0
                    }
                    line?.start()

                    while (offset < byteArray.size && isActive) {
                        val chunkSize = minOf(bufferSize, byteArray.size - offset)

                        barBuffer?.write(byteArray, offset, chunkSize)
                        playedBytes += chunkSize


                        // Process chunk for visualization based on playback progress
                        if (playedBytes >= bytesPerBar) {
                            processChunk(byteArray = barBuffer?.toByteArray())
                            barBuffer?.reset()
                            playedBytes -= bytesPerBar
                        }

                        line?.write(byteArray, offset, chunkSize)
                        offset += chunkSize
                    }

                    discard()
                }
            }

            override fun pause() {
                scope.coroutineContext.cancelChildren()
                line?.stop()
            }

            override fun discard() {
                onFinish()
                scope.coroutineContext.cancelChildren()
                runBlocking {
                    line?.drain()
                    line?.stop()
                    line?.close()
                    line?.flush()
                    line = null
                    offset = 0
                    playedBytes = 0
                }
            }
        }
    }
}

actual fun getMinBufferSize(sampleRate: Int, channels: Int, encoding: Int): Int {
    val bytesPerSample = when (encoding) {
        16 -> 2
        8 -> 1
        else -> throw IllegalArgumentException("Unsupported encoding: $encoding")
    }
    val estimatedFramesPerBuffer = (sampleRate * 0.02).toInt() // Estimate for 20ms buffer
    return estimatedFramesPerBuffer * channels * bytesPerSample
}
