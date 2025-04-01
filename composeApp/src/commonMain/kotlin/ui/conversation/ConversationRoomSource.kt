package ui.conversation

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.social.network.conversation.message.ConversationMessageIO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

data class GetMessagesResponse(
    val data: List<ConversationMessageIO> = listOf(),
    val hasNext: Boolean = false
)

/** factory for making paging requests */
class ConversationRoomSource(
    private val size: Int,
    private val getCount: suspend () -> Int,
    private val getMessages: suspend (page: Int) -> GetMessagesResponse?
): PagingSource<Int, ConversationMessageIO>() {

    override fun getRefreshKey(state: PagingState<Int, ConversationMessageIO>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    private var mutex = Mutex()
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ConversationMessageIO> {
        return try {
            mutex.withLock {
                val paramsKey = params.key ?: 0
                val response = getMessages(paramsKey)

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
