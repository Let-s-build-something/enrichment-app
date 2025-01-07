package base.utils.audio

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Audio player for playing a byteArray */
abstract class AudioPlayer(
    private val byteArray: ByteArray,
    scope: CoroutineScope,
    barsCount: Int,
    secondsPerBar: Double,
    bufferSize: Int
): AudioProcessor(
    secondsPerBar = secondsPerBar,
    sampleRate = bufferSize,
    barsCount = barsCount
) {
    // preprocess just enough for the component size
    init {
        val initialBoundary = barsCount * bytesPerBar / 2

        if(byteArray.size > initialBoundary) {
            scope.launch(Dispatchers.Default) {
                processChunk(byteArray.copyOfRange(0, initialBoundary))
            }
        }
    }

    /**
     * Starts playing the audio and initializes the player if it is not already,
     * otherwise, it just resumes the playing.
     */
    abstract fun play()

    /** Pauses the audio player, can be resumed by calling [play] again. */
    abstract fun pause()

    /** Stops and discards the audio player. */
    abstract fun discard()
}

/**
 * Automatically disposed audio player
 * @param secondsPerBar how many seconds should be represented by a single bar
 */
@Composable
expect fun rememberAudioPlayer(
    byteArray: ByteArray,
    onFinish: () -> Unit,
    barsCount: Int,
    sampleRate: Int = 44100,
    secondsPerBar: Double = 0.1,
    bufferSize: Int = sampleRate / 20
): AudioPlayer
