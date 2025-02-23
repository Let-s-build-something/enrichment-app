package ui.conversation

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.network.HttpException
import data.io.social.network.conversation.message.ConversationMessageIO
import kotlinx.io.IOException
import ui.conversation.MessagesRemoteMediator.Companion.INITIAL_BATCH

/** factory for making paging requests */
class ConversationRoomSource(
    private val size: Int,
    private val getMessages: suspend (batch: String) -> List<ConversationMessageIO>
): PagingSource<String, ConversationMessageIO>() {

    override fun getRefreshKey(state: PagingState<String, ConversationMessageIO>): String? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.data?.firstOrNull()?.currentBatch
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ConversationMessageIO> {
        return try {
            val paramsKey = params.key ?: INITIAL_BATCH
            val response = getMessages(paramsKey)
            val firstItem = response.firstOrNull()

            val nextKey = firstItem?.prevBatch?.takeIf { it != paramsKey }
            val prevKey = (firstItem?.nextBatch ?: INITIAL_BATCH.takeIf { paramsKey != INITIAL_BATCH })?.takeIf {
                it != firstItem?.prevBatch && it != paramsKey
            }
            val itemsAfter = if((response.size == size || paramsKey == INITIAL_BATCH) && nextKey != null) size else COUNT_UNDEFINED

            println("kostka_test, size: ${response.size}")
            println("kostka_test, params key: $paramsKey")
            println("kostka_test, prevKey: $prevKey")
            println("kostka_test, nextKey: $nextKey")
            println("kostka_test, itemsAfter: $itemsAfter")

            LoadResult.Page(
                data = response,
                prevKey = prevKey,
                nextKey = nextKey.takeIf { itemsAfter >= 0 },
                itemsAfter = itemsAfter
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
