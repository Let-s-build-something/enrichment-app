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
    private val lastBatch: () -> String?,
    private val findPreviousBatch: suspend (batch: String) -> String?,
    private val countItems: suspend (batch: String) -> Int,
    private val getMessages: suspend (batch: String) -> List<ConversationMessageIO>
): PagingSource<String, ConversationMessageIO>() {

    companion object {
        const val INITIAL_BATCH = "initial_batch"
    }

    override fun getRefreshKey(state: PagingState<String, ConversationMessageIO>): String? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.currentBatch ?: INITIAL_BATCH
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ConversationMessageIO> {
        return try {
            val paramsKey = params.key ?: INITIAL_BATCH

            val response = getMessages(paramsKey)
            val firstItem = response.firstOrNull()

            val nextKey = firstItem?.prevBatch?.takeIf { it != paramsKey }
            val prevKey = if(paramsKey != INITIAL_BATCH) {
                firstItem?.nextBatch?.takeIf {
                    it != firstItem.prevBatch && it != paramsKey
                } ?: (if(paramsKey != INITIAL_BATCH) findPreviousBatch(paramsKey) else null)
                ?: INITIAL_BATCH.takeIf {
                    paramsKey != INITIAL_BATCH && response.isNotEmpty()
                }
            }else null
            val itemsAfter = if(response.isNotEmpty() && nextKey != null
                && paramsKey != lastBatch()
                && nextKey != lastBatch()
            ) {
                countItems(nextKey).takeIf { it != 0 } ?: size
            } else COUNT_UNDEFINED
            val itemsBefore = if(prevKey != null) {
                countItems(prevKey).takeIf { it != 0 } ?: size
            }else COUNT_UNDEFINED

            LoadResult.Page(
                data = response,
                prevKey = prevKey.takeIf { paramsKey != INITIAL_BATCH },
                nextKey = nextKey.takeIf { itemsAfter > 0 },
                itemsAfter = itemsAfter,
                itemsBefore = itemsBefore
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
