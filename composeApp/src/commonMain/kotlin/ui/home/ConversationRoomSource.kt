package ui.home

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.SyncResponse
import kotlinx.io.IOException
import ui.home.HomeRepository.Companion.INITIAL_BATCH

/** factory for making paging requests */
class ConversationRoomSource(
    private val size: Int,
    private val getItems: suspend (batch: String?) -> BaseResponse<SyncResponse>
): PagingSource<String, ConversationRoomIO>() {

    override fun getRefreshKey(state: PagingState<String, ConversationRoomIO>): String? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.data?.firstOrNull()?.batch
                ?: state.closestPageToPosition(it)?.data?.firstOrNull()?.batch
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ConversationRoomIO> {
        return try {
            val response = getItems(params.key ?: INITIAL_BATCH)
            val data = response.success?.data ?: return LoadResult.Error(
                Throwable(message = response.error?.errors?.firstOrNull())
            )
            val content = mutableListOf<ConversationRoomIO>().apply {
                data.rooms?.let { response ->
                    addAll(response.join.values)
                    addAll(response.invite.values)
                    addAll(response.knock.values)
                    addAll(response.leave.values)
                }
            }

            LoadResult.Page(
                data = content,
                prevKey = null,
                nextKey = response.success?.data?.nextBatch,
                itemsAfter = if(content.size == size) {
                    COUNT_UNDEFINED
                }else 0
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
