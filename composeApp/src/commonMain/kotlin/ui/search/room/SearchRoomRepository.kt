package ui.search.room

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
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
    private var currentPagingSource: PagingSource<*, *>? = null
    private var itemsCount: Int = 0

    /** Attempts to invalidate local PagingSource with conversation messages */
    fun invalidateLocalSource() {
        itemsCount = 0
        currentPagingSource?.invalidate()
    }

    fun getRooms(
        query: () -> String,
        homeserver: () -> String,
        queryHomeserver: () -> String?,
        config: PagingConfig
    ): Pager<String, GetPublicRoomsResponse.PublicRoomsChunk> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                SearchRoomSource(
                    getRooms = { batch ->
                        queryRooms(
                            query = query(),
                            limit = config.pageSize,
                            homeserver = homeserver(),
                            queryHomeserver = queryHomeserver(),
                            since = batch
                        ).data?.also {
                            itemsCount += it.chunk.size
                        }
                    },
                    getCount = { itemsCount },
                    size = config.pageSize
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            }
        )
    }

    suspend fun queryRooms(
        query: String,
        limit: Int,
        homeserver: String,
        queryHomeserver: String?,
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
                queryHomeserver?.let { parameter("server", it) }
            }
        }
    }
}
