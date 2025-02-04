package ui.conversation.components.audio

import base.utils.sha256
import database.file.FileAccess
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path

/** Class for calling APIs and remote work in general */
class MediaProcessorRepository(
    private val fileAccess: FileAccess,
    private val httpClient: HttpClient
) {

    /** returns a file from Google cloud storage or cache and caches it if it is not cached */
    suspend fun getFileByteArray(
        url: String,
        downloadUrl: String = url,
        extension: String? = null,
        onProgressChange: (bytesSentTotal: Long, contentLength: Long?) -> Unit
    ): Pair<ByteArray, Path?>? {
        return withContext(Dispatchers.IO) {
            val fileName = sha256(url).plus(if(extension != null) ".$extension" else "")

            if(url.isBlank()) return@withContext null

            fileAccess.loadFileFromCache(fileName) ?: try {
                if(downloadUrl.isBlank()) return@withContext null

                val byteArray = httpClient.get(urlString = downloadUrl) {
                    onDownload { bytesSentTotal, contentLength ->
                        onProgressChange(bytesSentTotal, contentLength)
                    }
                }.bodyAsBytes()

                byteArray.takeIf { it.isNotEmpty() }.let {
                    if(it == null) null
                    else it to fileAccess.saveFileToCache(
                        data = it,
                        fileName = fileName
                    )
                }
            }catch (e: Exception) { null }
        }
    }

    /** Returns the content of a url as a text */
    suspend fun getUrlContent(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                HttpClient().config {
                    install(ContentNegotiation)
                }.get(urlString = url).bodyAsText()
            }catch (e: Exception) {
                null
            }
        }
    }
}
