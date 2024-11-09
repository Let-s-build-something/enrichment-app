package ui.network.received

import androidx.paging.PagingSource
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.social.network.request.CirclingRequest
import data.io.social.network.request.CirclingRequestsResponse
import kotlinx.io.IOException

/** factory for making paging requests */
class NetworkReceivedSource(
    private val size: Int,
    private val getRequests: suspend (page: Int, size: Int) -> BaseResponse<CirclingRequestsResponse>
): PagingSource<Int, CirclingRequest>() {

    override fun getRefreshKey(state: PagingState<Int, CirclingRequest>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CirclingRequest> {
        return try {
            val response = getRequests(params.key ?: 0, size)
            val data = response.success?.data ?: return LoadResult.Error(
                Throwable(message = response.error?.errors?.firstOrNull())
            )

            LoadResult.Page(
                data = data.content,
                prevKey = if(data.pagination.page > 0) {
                    data.pagination.page.minus(1)
                } else null,
                nextKey = if(data.pagination.page < data.pagination.totalPages - 1) {
                    data.pagination.page.plus(1)
                } else null
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}