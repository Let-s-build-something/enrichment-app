package ui.conversation.components.audio

import augmy.interactive.com.BuildKonfig
import data.io.base.BaseResponse
import data.io.social.network.conversation.ConversationMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
        // TODO check for local cache

        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ConversationMessagesResponse> {
                get(
                    url = Url(buildString {
                        append(url)
                    }),
                    block = {
                        parameter("key", BuildKonfig.CloudWebApiKey)
                    }
                )
            }.also {
                println("kostka_test, getFile result: ${it.success?.data}")
            }
        }
    }
}