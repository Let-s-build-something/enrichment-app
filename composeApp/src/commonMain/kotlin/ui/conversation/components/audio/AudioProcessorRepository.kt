package ui.conversation.components.audio

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class AudioProcessorRepository {

    /** returns an audio wav file from Google cloud storage */
    suspend fun getAudioBytes(url: String): ByteArray? {
        // TODO check for local cache

        return withContext(Dispatchers.IO) {
            try {
                HttpClient().config {
                    install(ContentNegotiation)
                }.get(urlString = url).bodyAsBytes()
            }catch (e: Exception) { null }
        }
    }
}
