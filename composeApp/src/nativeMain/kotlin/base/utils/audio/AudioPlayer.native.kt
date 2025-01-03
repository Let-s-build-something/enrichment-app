package base.utils.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import okio.Buffer
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.darwin.ByteVar
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync
import platform.posix.memcpy

/**
 * Automatically disposed audio player
 * @param secondsPerBar how many seconds should be represented by a single bar
 */
@OptIn(ExperimentalForeignApi::class)
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
            private var engine: AVAudioEngine? = null
            private var playerNode: AVAudioPlayerNode? = null
            private var barBuffer: Buffer? = null
            private val format = AVAudioFormat(sampleRate.toDouble(), 1u)
            private var playedBytes = 0

            override fun play() {
                scope.launch {
                    if (engine == null) {
                        playedBytes = 0
                        engine = AVAudioEngine()
                        barBuffer = Buffer()
                        playerNode = AVAudioPlayerNode().also {
                            engine?.attachNode(it)
                            engine?.mainMixerNode?.let { mixer ->
                                engine?.connect(it, mixer, format)
                            }
                        }

                        engine?.prepare()
                        engine?.startAndReturnError(null)
                    }

                    val audioBuffer = AVAudioPCMBuffer(
                        AVAudioFormat(sampleRate.toDouble(), 1u),
                        byteArray.size.toUInt()
                    )
                    audioBuffer.frameLength = (byteArray.size / 2).toUInt()
                    byteArray.usePinned { pinnedBytes ->
                        audioBuffer.int16ChannelData?.get(0)?.reinterpret<ShortVar>()?.let { destPointer ->
                            memcpy(destPointer, pinnedBytes.addressOf(0), byteArray.size.convert())
                        }
                    }

                    engine?.mainMixerNode?.installTapOnBus(
                        bus = 0u,
                        bufferSize = bufferSize.toUInt(),
                        format = format
                    ) { buffer, _ ->
                        handleAudioBuffer(buffer)
                    }

                    playerNode?.scheduleBuffer(audioBuffer) {
                        dispatch_sync(dispatch_get_main_queue()) {
                            discard()
                        }
                    }

                    playerNode?.play()
                }
            }

            override fun pause() {
                playerNode?.pause()
            }

            override fun discard() {
                onFinish()
                AVAudioSession.sharedInstance().setActive(
                    active = false,
                    nativeHeap.allocPointerTo<ObjCObjectVar<NSError?>>().value
                )
                scope.coroutineContext.cancelChildren()
                playerNode?.stop()
                engine?.stop()
                playedBytes = 0
                playerNode = null
                engine = null
                barBuffer = null
            }

            private fun handleAudioBuffer(buffer: AVAudioPCMBuffer?) {
                if (buffer != null) {
                    val frameLength = buffer.frameLength.toInt()
                    val byteData = ByteArray(frameLength * 2) // Assuming 16-bit PCM data

                    buffer.int16ChannelData?.get(0)?.reinterpret<ByteVar>()?.let { sourcePointer ->
                        byteData.usePinned { pinnedByteArray ->
                            memcpy(
                                pinnedByteArray.addressOf(0),
                                sourcePointer,
                                byteData.size.convert()
                            )
                        }
                    }
                    barBuffer?.write(byteData)
                    playedBytes += byteData.size

                    if (playedBytes >= bytesPerBar) {
                        scope.launch {
                            processChunk(barBuffer?.readByteArray())
                        }
                        barBuffer?.clear()
                        playedBytes -= bytesPerBar
                    }
                }
            }
        }
    }
}
