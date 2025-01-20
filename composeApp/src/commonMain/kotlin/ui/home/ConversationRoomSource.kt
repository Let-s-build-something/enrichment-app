package ui.home

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.social.network.request.NetworkListResponse
import data.io.user.NetworkItemIO
import data.io.user.matrix.SyncResponse
import kotlinx.io.IOException

/** factory for making paging requests */
class ConversationRoomSource(
    private val getItems: suspend (batch: String?) -> BaseResponse<SyncResponse>
): PagingSource<String, ConversationRoomIO>() {

    override fun getRefreshKey(state: PagingState<String, ConversationRoomIO>): String? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey
                ?: state.closestPageToPosition(it)?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ConversationRoomIO> {
        return try {
            val response = getItems(params.key)
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
