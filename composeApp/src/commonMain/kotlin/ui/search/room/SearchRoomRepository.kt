package ui.search.room

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsResponse
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsWithFilter
import ui.login.safeRequest

class SearchRoomRepository(
    private val httpClient: HttpClient
) {

    suspend fun queryRooms(
        query: String,
        limit: Int,
        homeserver: String,
        queryHomeserver: String,
        since: String? = null
    ) = withContext(Dispatchers.IO) {
        httpClient.safeRequest<GetPublicRoomsResponse> {
            post(url = Url("https://$homeserver/_matrix/client/v3/publicRooms")) {
                setBody(
                    GetPublicRoomsWithFilter.Request(
                        limit = limit.toLong(),
                        since = since,
                        filter = GetPublicRoomsWithFilter.Request.Filter(
                            genericSearchTerm = query
                        )
                    )
                )
                parameter("server", queryHomeserver)
            }
        }
    }
}
