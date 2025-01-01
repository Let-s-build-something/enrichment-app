package ui.conversation.components.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val audioProcessorModule = module {
    factory { AudioProcessorModel(get()) }
    factory { AudioProcessorRepository() }
    viewModelOf(::AudioProcessorModel)
}

/** Audio processor model for downloading the audio files */
class AudioProcessorModel(
    private val repository: AudioProcessorRepository
): ViewModel() {
    private val _resultByteArray = MutableStateFlow<ByteArray?>(null)

    /** Result of the downloaded byte array from an url */
    val resultByteArray: StateFlow<ByteArray?> = _resultByteArray.asStateFlow()

    /** Download the remote [ByteArray] by [url] */
    fun downloadByteArray(url: String) {
        viewModelScope.launch {
            repository.getAudioBytes(url)?.let { file ->
                // wav to PCM
                _resultByteArray.value = file.copyOfRange(44, file.size)
            }
        }
    }
}