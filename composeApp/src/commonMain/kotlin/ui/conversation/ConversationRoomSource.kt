package ui.conversation

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import kotlinx.io.IOException

/** factory for making paging requests */
class ConversationRoomSource(
    private val size: Int,
    private val getMessages: suspend (page: Int) -> BaseResponse<ConversationMessagesResponse>
): PagingSource<Int, ConversationMessageIO>() {

    override fun getRefreshKey(state: PagingState<Int, ConversationMessageIO>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ConversationMessageIO> {
        return try {
            val response = getMessages(params.key ?: 0)
            val data = response.success?.data ?: return LoadResult.Error(
                Throwable(message = response.error?.errors?.firstOrNull())
            )

            LoadResult.Page(
                data = data.content,
                prevKey = if(data.pagination.page > 0) {
                    data.pagination.page.minus(1)
                } else null,
                nextKey = if(data.content.size == size) {
                    data.pagination.page.plus(1)
                } else null,
                itemsAfter = if(data.content.size == size) {
                    (data.pagination.totalItems - (data.pagination.page + 1).times(size)).coerceAtLeast(0)
                }else COUNT_UNDEFINED
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
