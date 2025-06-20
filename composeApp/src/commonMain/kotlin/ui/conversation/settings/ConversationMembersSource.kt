package ui.conversation.settings

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.matrix.room.event.ConversationRoomMember
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

data class GetMembersResponse(
    val data: List<ConversationRoomMember> = listOf(),
    val hasNext: Boolean = false
)

/** factory for making paging requests */
class ConversationMembersSource(
    private val size: Int,
    private val getCount: suspend () -> Int,
    private val getMembers: suspend (page: Int) -> GetMembersResponse?
): PagingSource<Int, ConversationRoomMember>() {

    override fun getRefreshKey(state: PagingState<Int, ConversationRoomMember>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    private var mutex = Mutex()
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ConversationRoomMember> {
        return try {
            mutex.withLock {
                val paramsKey = params.key ?: 0
                val response = getMembers(paramsKey)

                if(response != null) {
                    LoadResult.Page(
                        data = response.data,
                        prevKey = if(paramsKey > 0) paramsKey.minus(1) else null,
                        nextKey = if(response.hasNext) paramsKey.plus(1) else null,
                        itemsAfter = if(response.hasNext) {
                            getCount().minus(paramsKey.plus(1) * size).takeIf { it > 0 } ?: size
                        }else COUNT_UNDEFINED,
                        itemsBefore = if(paramsKey > 0) paramsKey.minus(1) * size else COUNT_UNDEFINED
                    )
                }else LoadResult.Error(Throwable("No response"))
            }
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}
