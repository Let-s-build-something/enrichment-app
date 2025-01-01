package base.utils.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

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
        object: AudioPlayer(
            byteArray = byteArray,
            barsCount = barsCount,
            secondsPerBar = secondsPerBar,
            bufferSize = bufferSize,
            scope = scope
        ) {
            override fun play() {
                TODO("Not yet implemented")
            }

            override fun pause() {
                TODO("Not yet implemented")
            }

            override fun discard() {
                TODO("Not yet implemented")
            }
        }
    }
}