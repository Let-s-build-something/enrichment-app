package ui.conversation.components.audio

import base.utils.getExtensionFromMimeType
import base.utils.sha256
import database.file.FileAccess
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path

/** Class for calling APIs and remote work in general */
class MediaProcessorRepository(
    private val fileAccess: FileAccess,
    private val httpClient: HttpClient
) {

    class FileResult(
        val byteArray: ByteArray,
        val path: Path?,
        val mimetype: String?
    )

    /** returns a file from Google cloud storage or cache and caches it if it is not cached */
    suspend fun getFileByteArray(
        url: String,
        downloadUrl: String = url,
        extension: String? = null,
        onProgressChange: (bytesSentTotal: Long, contentLength: Long?) -> Unit
    ): FileResult? {
        return withContext(Dispatchers.IO) {
            val fileName = sha256(url).plus(if(extension != null) ".$extension" else "")

            if(url.isBlank()) return@withContext null

            fileAccess.loadFileFromCache(fileName = fileName, extension = extension) ?: try {
                if(downloadUrl.isBlank()) return@withContext null

                val response = httpClient.get(urlString = downloadUrl) {
                    onDownload { bytesSentTotal, contentLength ->
                        onProgressChange(bytesSentTotal, contentLength)
                    }
                }
                val mimetype = response.contentType()?.toString()

                response.bodyAsBytes().takeIf { it.isNotEmpty() }.let { bytes ->
                    if(bytes == null) null
                    else FileResult(
                        byteArray = bytes,
                        path = fileAccess.saveFileToCache(
                            data = bytes,
                            fileName = fileName.plus(getExtensionFromMimeType(mimetype)?.let { ".$it" })
                        ),
                        mimetype = mimetype
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
