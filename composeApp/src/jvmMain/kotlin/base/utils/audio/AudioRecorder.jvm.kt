package base.utils.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/** Automatically disposed audio recorder */
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
            // audio recording
            private val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            private val lineInfo = DataLine.Info(TargetDataLine::class.java, format)
            private var line: TargetDataLine? = null
            private var buffer: ByteArrayOutputStream? = null
            private var recordingThread: Thread? = null

            // bar creation
            private var barBuffer: ByteArrayOutputStream? = null

            override fun startRecording() {
                if (!AudioSystem.isLineSupported(lineInfo)) return

                if(line == null) {
                    stopRecording()

                    buffer = ByteArrayOutputStream()
                    barBuffer = ByteArrayOutputStream()

                    line = getMicrophoneLine(format = format)
                }

                if(line?.isOpen == true) {
                    recordingThread = Thread {
                        val audioStream = AudioInputStream(line)
                        val data = ByteArray(
                            bufferSize / audioStream.format.frameSize.times(2) * audioStream.format.frameSize.times(2)
                        )

                        while (recordingThread != null) {
                            val bytesRead = audioStream.read(data, 0, data.size)

                            if (bytesRead > 0) {
                                buffer?.write(data, 0, bytesRead)
                                barBuffer?.write(data, 0, bytesRead)

                                if((barBuffer?.size() ?: 0) >= bytesPerBar) {
                                    val byteArray = barBuffer?.toByteArray()
                                    coroutineScope.launch {
                                        processChunk(byteArray = byteArray)
                                    }
                                    barBuffer?.reset()
                                }
                            }
                        }
                    }
                    recordingThread?.priority = Thread.MAX_PRIORITY
                    recordingThread?.start()
                    line?.start()
                }
            }

            override suspend fun saveRecording(): ByteArray? {
                return pcmToWav(
                    pcmData = buffer?.toByteArray(),
                    channels = 1,
                    bitsPerSample = 16
                )
            }

            override fun pauseRecording() {
                line?.stop()
                recordingThread?.interrupt()
                recordingThread = null
            }

            override fun stopRecording() {
                super.stopRecording()
                line?.stop()
                line?.close()
                line?.flush()
                line = null
                buffer?.flush()
                buffer = null
                recordingThread?.interrupt()
                recordingThread = null
                barBuffer?.flush()
                barBuffer = null
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

private fun getMicrophoneLine(format: AudioFormat): TargetDataLine? {
    val mixers = AudioSystem.getMixerInfo()

    for (mixerInfo in mixers) {
        val mixer = AudioSystem.getMixer(mixerInfo)

        for (lineInfo in mixer.targetLineInfo) {
            if (lineInfo is DataLine.Info
                && TargetDataLine::class.java.isAssignableFrom(lineInfo.lineClass)
                && lineInfo.isFormatSupported(format)
            ) {
                val line = mixer.getLine(lineInfo) as TargetDataLine
                line.open(format)
                return line
            }
        }
    }

    return null
}
