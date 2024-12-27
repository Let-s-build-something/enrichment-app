package base.utils.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Audio player for playing a byteArray */
class AudioPlayer(
    private val byteArray: ByteArray,
    barsCount: Int,
    secondsPerBar: Double,
    bufferSize: Int
): AudioProcessor(
    secondsPerBar = secondsPerBar,
    sampleRate = bufferSize,
    barsCount = barsCount
) {
    /**
     * Starts playing the audio and initializes the player if it is not already,
     * otherwise, it just resumes the playing.
     */
    fun play() {
        TODO("Not yet implemented")
    }

    /** Pauses the audio player, can be resumed by calling [play] again. */
    fun pause() {
        TODO("Not yet implemented")
    }

    /** Stops and discards the audio player. */
    fun discard() {
        TODO("Not yet implemented")
    }
}

/**
 * Automatically disposed audio player
 * @param secondsPerBar how many seconds should be represented by a single bar
 */
@Composable
fun rememberAudioPlayer(
    key: Any? = null,
    byteArray: ByteArray,
    barsCount: Int,
    sampleRate: Int = 44100,
    secondsPerBar: Double = 0.1,
    bufferSize: Int = sampleRate / 21
): AudioPlayer {
    return remember(key) {
        AudioPlayer(
            byteArray = byteArray,
            barsCount = barsCount,
            secondsPerBar = secondsPerBar,
            bufferSize = bufferSize
        )
    }
}
