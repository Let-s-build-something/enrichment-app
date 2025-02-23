package ui.conversation

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.social.network.conversation.message.ConversationMessageIO
import kotlinx.io.IOException

/** factory for making paging requests */
class ConversationRoomSource(
    private val size: Int,
    private val initialBatch: suspend () -> String,
    private val getMessages: suspend (batch: String) -> List<ConversationMessageIO>
): PagingSource<String, ConversationMessageIO>() {

    override fun getRefreshKey(state: PagingState<String, ConversationMessageIO>): String? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.data?.firstOrNull()?.nextBatch
                ?: state.closestPageToPosition(it)?.data?.firstOrNull()?.prevBatch
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ConversationMessageIO> {
        return try {
            val response = getMessages(params.key ?: initialBatch())
            val firstItem = response.firstOrNull()

            LoadResult.Page(
                data = response,
                prevKey = firstItem?.prevBatch,
                nextKey = firstItem?.nextBatch,
                itemsAfter = if(response.size == size) size else COUNT_UNDEFINED
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
