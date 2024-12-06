package base.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.Foundation.NSError
import platform.Foundation.NSMutableData
import platform.Foundation.appendBytes

/** Automatically disposed audio recorder */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberAudioRecorder(
    barsCount: Int,
    sampleRate: Int,
    secondsPerBar: Double,
    bufferSize: Int
): AudioRecorder {
    val coroutineScope = rememberCoroutineScope()

    val audioRecorder = remember {
        object: AudioRecorder(
            sampleRate = sampleRate,
            secondsPerBar = secondsPerBar,
            barsCount = barsCount
        ) {
            private var audioEngine: AVAudioEngine? = null
            private var byteOutputStream: NSMutableData? = null
            private var barByteOutputStream: NSMutableData? = null

            override fun startRecording() {
                if(audioEngine == null) {
                    stopRecording()
                    audioEngine = AVAudioEngine()
                }
                audioEngine?.inputNode?.let { inputNode ->
                    if(audioEngine == null) {
                        val buffer = AVAudioPCMBuffer(inputNode.outputFormatForBus(0u), frameCapacity = (sampleRate * 2).toUInt())
                        byteOutputStream = NSMutableData()
                        barByteOutputStream = NSMutableData()

                        inputNode.installTapOnBus(0u, bufferSize = bufferSize.toUInt(), format = buffer.format) { tapBuffer, _ ->
                            val data = tapBuffer?.audioBufferList?.get(0)?.mBuffers?.get(0)?.mData
                            val size = tapBuffer?.audioBufferList?.get(0)?.mBuffers?.get(0)?.mDataByteSize?.toInt() ?: 0
                            byteOutputStream?.appendBytes(data, size.toULong())
                            barByteOutputStream?.appendBytes(data, size.toULong())

                            if((barByteOutputStream?.length?.toInt() ?: 0) >= bytesPerBar) {
                                val byteArray = barByteOutputStream?.byteArray
                                coroutineScope.launch {
                                    processChunk(byteArray = byteArray)
                                }
                                barByteOutputStream?.setLength(0u)
                            }
                        }

                        audioEngine?.prepare()
                    }
                    audioEngine?.startAndReturnError(nativeHeap.allocPointerTo<ObjCObjectVar<NSError?>>().value)
                }
            }

            override suspend fun saveRecording(): ByteArray? {
                return withContext(Dispatchers.Default) {
                    byteOutputStream?.byteArray
                }
            }

            override fun pauseRecording() {
                audioEngine?.pause()
            }

            override fun stopRecording() {
                super.stopRecording()
                audioEngine?.stop()
                audioEngine?.inputNode?.removeTapOnBus(0u)
                audioEngine = null
                byteOutputStream?.setLength(0u)
                barByteOutputStream?.setLength(0u)
                byteOutputStream = null
                barByteOutputStream = null
            }
        }
    }

    DisposableEffect(null) {
        onDispose {
            audioRecorder.stopRecording()
        }
    }

    return audioRecorder
}

/** Returns [ByteArray] from [NSMutableData] */
@OptIn(ExperimentalForeignApi::class)
val NSMutableData?.byteArray: ByteArray?
    get() {
        return this?.let { outputStream ->
            outputStream.bytes?.usePinned {
                it.get().readBytes(outputStream.length.toInt())
            }
        }
    }
