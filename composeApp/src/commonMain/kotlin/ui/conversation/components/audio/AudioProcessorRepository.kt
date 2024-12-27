package ui.conversation.components.audio

import data.io.base.BaseResponse
import data.io.social.network.conversation.ConversationMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class AudioProcessorRepository(
    private val httpClient: HttpClient
) {

    /** returns a file from Google cloud storage */
    suspend fun getFile(url: String): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ConversationMessagesResponse> {
                get(
                    url = Url(buildString {
                        append(url)
                        // TODO API key
                    })
                )
            }
        }
    }
}