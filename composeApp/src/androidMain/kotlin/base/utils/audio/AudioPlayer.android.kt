package base.utils.audio

import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SPEECH
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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
    val scope = rememberCoroutineScope()

    return remember(byteArray.size) {
        object : AudioPlayer(
            byteArray = byteArray,
            barsCount = barsCount,
            secondsPerBar = secondsPerBar,
            bufferSize = bufferSize,
            scope = scope
        ) {
            private var offset = 0
            private var playedBytes = 0
            private var audioTrack: AudioTrack? = null

            private var barBuffer: ByteArrayOutputStream? = null

            override fun play() {
                scope.launch {
                    if(audioTrack == null) {
                        audioTrack = AudioTrack(
                            AudioAttributes.Builder()
                                .setUsage(USAGE_MEDIA)
                                .setContentType(CONTENT_TYPE_SPEECH)
                                .build(),
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                            bufferSize,
                            AudioTrack.MODE_STREAM,
                            AudioManager.AUDIO_SESSION_ID_GENERATE
                        )
                        barBuffer = ByteArrayOutputStream()
                    }

                    audioTrack?.play()

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

                        audioTrack?.write(byteArray, offset, chunkSize)
                        offset += chunkSize
                    }

                    discard()
                }
            }

            override fun pause() {
                audioTrack?.stop()
            }

            override fun discard() {
                onFinish()
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack?.flush()
                barBuffer?.reset()
                offset = 0
                playedBytes = 0
                audioTrack = null
                barBuffer = null
            }
        }
    }
}

actual fun getMinBufferSize(
    sampleRate: Int,
    channels: Int,
    encoding: Int
): Int = AudioTrack.getMinBufferSize(
    sampleRate,
    AudioFormat.CHANNEL_OUT_MONO,
    AudioFormat.ENCODING_PCM_16BIT
)
