package ui.login.homeserver_picker

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ui.login.safeRequest

class HomeserverPickerRepository(private val httpClient: HttpClient) {
    @Serializable
    data class WellKnownServer(
        @SerialName("m.server")
        val server: String? = null
    )

    suspend fun getWellKnown(address: String) = withContext(Dispatchers.IO) {
        httpClient.safeRequest<WellKnownServer> {
            get("https://$address/.well-known/matrix/server")
        }
    }

    suspend fun validateHomeserver(homeserver: String) = withContext(Dispatchers.IO) {
        httpClient.safeRequest<Any> { 
            get("https://$homeserver/_matrix/client/versions")
        }
    }
}