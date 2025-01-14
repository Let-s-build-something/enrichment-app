package ui.conversation.components.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import database.file.FileAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val audioProcessorModule = module {
    factory { MediaProcessorModel(get()) }
    factory { MediaProcessorRepository(get<FileAccess>()) }
    viewModelOf(::MediaProcessorModel)
}

data class MediaHttpProgress(
    val id: String = "",
    val items: Int,
    val item: Int,
    val progress: LongRange?
)

/** Audio processor model for downloading the audio files */
class MediaProcessorModel(
    private val repository: MediaProcessorRepository
): ViewModel() {
    private val _resultByteArray = MutableStateFlow<ByteArray?>(null)
    private val _resultData = MutableStateFlow<Map<String, ByteArray>>(mapOf())
    private val _downloadProgress = MutableStateFlow<MediaHttpProgress?>(null)

    /** Result of the downloaded byte array from an url */
    val resultByteArray = _resultByteArray.asStateFlow()

    /** Result of the downloaded data from an url */
    val resultData = _resultData.asStateFlow()

    /** Progress of the current download */
    val downloadProgress = _downloadProgress.asStateFlow()

    /** Download the remote [ByteArray] by [url] */
    fun downloadAudioByteArray(url: String) {
        viewModelScope.launch {
            repository.getFileByteArray(
                url = url,
                onProgressChange = { bytesSentTotal, contentLength ->
                    _downloadProgress.value = MediaHttpProgress(
                        items = 1,
                        item = 1,
                        progress = if(contentLength == null) null else (bytesSentTotal..contentLength)
                    )
                }
            )?.let { file ->
                // wav to PCM
                _resultByteArray.value = file.copyOfRange(44, file.size)
            }
        }
    }

    /** Flushes the cached downloaded data */
    fun flush() {
        _resultByteArray.value = null
        _resultData.value = mapOf()
        _downloadProgress.value = null
    }

    /** Attempts to retrieve bitmaps out of urls */
    fun processFiles(vararg urls: String?) {
        viewModelScope.launch {
            _downloadProgress.value = MediaHttpProgress(
                items = urls.size,
                item = 0,
                progress = null
            )
            _resultData.value = urls.mapIndexedNotNull { index, url ->
                (if(url == null) null else repository.getFileByteArray(
                    url = url,
                    onProgressChange = { bytesSentTotal, contentLength ->
                        _downloadProgress.value = MediaHttpProgress(
                            items = urls.size,
                            item = index + 1,
                            progress = if(contentLength == null) null else (bytesSentTotal..contentLength)
                        )
                    }
                )).let {
                    if(it == null || url == null) null else url to it
                }
            }.toMap()
            _downloadProgress.value = null
        }
    }
}