package ui.conversation.components.audio

import androidx.lifecycle.viewModelScope
import base.utils.Matrix.Media.MATRIX_REPOSITORY_PREFIX
import base.utils.getExtensionFromMimeType
import com.fleeksoft.ksoup.Ksoup
import data.io.social.network.conversation.message.MediaIO
import data.shared.SharedViewModel
import database.file.FileAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.components.link.GraphProtocol

internal val audioProcessorModule = module {
    factory { MediaProcessorDataManager() }
    single { MediaProcessorDataManager() }
    factory { MediaProcessorModel(get(), get()) }
    factory { MediaProcessorRepository(get<FileAccess>(), get()) }
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
    private val repository: MediaProcessorRepository,
    private val dataManager: MediaProcessorDataManager
): SharedViewModel() {
    private val _resultByteArray = MutableStateFlow<ByteArray?>(null)
    private val _resultData = MutableStateFlow<Map<MediaIO, ByteArray>>(mapOf())
    private val _downloadProgress = MutableStateFlow<MediaHttpProgress?>(null)
    private val _graphProtocol = MutableStateFlow<GraphProtocol?>(null)

    /** Result of the downloaded byte array from an url */
    val resultByteArray = _resultByteArray.asStateFlow()

    /** Result of the downloaded data from an url */
    val resultData = _resultData.asStateFlow()

    /** Progress of the current download */
    val downloadProgress = _downloadProgress.asStateFlow()

    /** Locally cached files mapped to local paths */
    val cachedFiles = dataManager.cachedFiles.asStateFlow()

    /** Resulting graph protocol from a website fetcher */
    val graphProtocol = _graphProtocol.asStateFlow()

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
                _resultByteArray.value = file.first.copyOfRange(44, file.first.size)
            }
        }
    }

    /** Flushes the cached downloaded data */
    fun flush() {
        _resultByteArray.value = null
        _resultData.value = mapOf()
        _downloadProgress.value = null
    }

    /** Attempts to retrieve bytearrays out of urls */
    fun downloadFiles(vararg media: MediaIO?) {
        viewModelScope.launch {
            _downloadProgress.value = MediaHttpProgress(
                items = media.size,
                item = 0,
                progress = null
            )
            var bytesSentTotal = 0
            val totalContentSize = media.sumOf { it?.size ?: 0 }
            _resultData.value = media.mapIndexedNotNull { index, unit ->
                (if(unit == null) null else repository.getFileByteArray(
                    url = unit.url ?: "",
                    onProgressChange = { bytesSent, _ ->
                        _downloadProgress.value = MediaHttpProgress(
                            items = media.size,
                            item = index + 1,
                            progress = (bytesSentTotal + bytesSent)..totalContentSize
                        )
                    }
                )).let {
                    bytesSentTotal += unit?.size ?: 0
                    if(it == null || unit == null) null else unit to it.first
                }
            }.toMap()
            _downloadProgress.value = null
        }
    }

    /** Attempts to retrieve bytearrays out of urls and cache them */
    fun cacheFiles(vararg media: MediaIO?) {
        viewModelScope.launch {
            _downloadProgress.value = MediaHttpProgress(
                items = media.size,
                item = 0,
                progress = null
            )
            var bytesSentTotal = 0
            val totalContentSize = media.sumOf { it?.size ?: 0 }
            media.forEachIndexed { index, unit ->
                val downloadUrl = if(unit?.url != null && currentUser.value?.matrixHomeserver != null) {
                    unit.url.takeIf { !it.startsWith(MATRIX_REPOSITORY_PREFIX) }
                        ?: "https://${currentUser.value?.matrixHomeserver}/_matrix/client/v1/media/download/${unit.url.replace(MATRIX_REPOSITORY_PREFIX, "")}"
                }else ""

                repository.getFileByteArray(
                    url = unit?.url ?: "",
                    downloadUrl = downloadUrl,
                    extension = getExtensionFromMimeType(unit?.mimetype),
                    onProgressChange = { bytesSent, _ ->
                        _downloadProgress.value = MediaHttpProgress(
                            items = media.size,
                            item = index + 1,
                            progress = (bytesSentTotal + bytesSent)..totalContentSize
                        )
                    }
                ).let { result ->
                    dataManager.cachedFiles.value = dataManager.cachedFiles.value.toMutableMap().apply {
                        if(result != null && unit != null) {
                            bytesSentTotal += unit.size ?: 0
                            put(
                                unit.url ?: "",
                                unit.copy(path = result.second?.toString())
                            )
                        }
                    }
                }
            }
            _downloadProgress.value = null
        }
    }

    /** Requests open graph protocol data out of an [url] */
    fun requestGraphProtocol(url: String) {
        viewModelScope.launch {
            Ksoup.parseMetaData(html = repository.getUrlContent(url) ?: "").let { metadata ->
                _graphProtocol.value = GraphProtocol(
                    title = metadata.ogTitle,
                    description = metadata.ogDescription,
                    imageUrl = metadata.ogImage,
                    iconUrl = metadata.favicon
                )
            }
        }
    }
}
