package ui.conversation.components.audio

import base.utils.sha256
import database.file.FileAccess
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class AudioProcessorRepository(
    private val fileAccess: FileAccess
) {

    /** returns an audio wav file from Google cloud storage */
    suspend fun getAudioBytes(url: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            fileAccess.loadFileFromCache(sha256(url)) ?: try {
                HttpClient().config {
                    install(ContentNegotiation)
                }.get(urlString = url).bodyAsBytes().also { byteArray ->
                    byteArray.takeIf { it.isNotEmpty() }?.let {
                        fileAccess.saveFileToCache(data = it, fileName = sha256(url))
                    }
                }
            }catch (e: Exception) { null }
        }
    }
}
