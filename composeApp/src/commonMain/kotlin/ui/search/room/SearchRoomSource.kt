package ui.search.room

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import kotlinx.io.IOException
import net.folivo.trixnity.clientserverapi.model.rooms.GetPublicRoomsResponse

/** factory for making paging requests */
class SearchRoomSource(
    private val size: Int,
    private val getCount: () -> Int,
    private val getRooms: suspend (batch: String?) -> GetPublicRoomsResponse?
): PagingSource<String, GetPublicRoomsResponse.PublicRoomsChunk>() {

    override fun getRefreshKey(state: PagingState<String, GetPublicRoomsResponse.PublicRoomsChunk>): String? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey
                ?: state.closestPageToPosition(it)?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, GetPublicRoomsResponse.PublicRoomsChunk> {
        return try {
            val response = getRooms(params.key)

            if(response != null) {
                LoadResult.Page(
                    data = response.chunk,
                    prevKey = response.prevBatch,
                    nextKey = response.nextBatch,
                    itemsAfter = if (response.nextBatch != null) {
                        response.totalRoomCountEstimate?.minus(getCount())?.toInt() ?: size
                    }else COUNT_UNDEFINED,
                    itemsBefore = COUNT_UNDEFINED
                )
            }else LoadResult.Error(Throwable("No response"))
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}
