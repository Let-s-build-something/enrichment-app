package ui.login.homeserver_picker

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ui.login.safeRequest

class HomeserverPickerRepository(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    @Serializable
    data class WellKnownServer(
        @SerialName("m.server")
        val server: String? = null
    )

    suspend fun getWellKnown(address: String) = withContext(Dispatchers.IO) {
        try {
            json.decodeFromString<WellKnownServer>(
                httpClient.get("https://$address/.well-known/matrix/server").bodyAsText()
            )
        } catch (_: Exception) { null }
    }

    suspend fun validateHomeserver(homeserver: String) = withContext(Dispatchers.IO) {
        httpClient.safeRequest<Any> { 
            get("https://$homeserver/_matrix/client/versions")
        }
    }
}