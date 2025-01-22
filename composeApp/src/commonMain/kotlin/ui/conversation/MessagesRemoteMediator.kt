package ui.conversation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.base.PagingEntityType
import data.io.base.PagingMetaIO
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import database.dao.ConversationMessageDao
import database.dao.PagingMetaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.io.IOException

/**
 * Mediator for reusing locally loaded data and fetching new data from the network if necessary
 */
@OptIn(ExperimentalPagingApi::class)
class MessagesRemoteMediator (
    private val conversationMessageDao: ConversationMessageDao,
    private val pagingMetaDao: PagingMetaDao,
    private val conversationId: String,
    private val size: Int,
    private val initialPage: Int = 0,
    private val invalidatePagingSource: () -> Unit,
    private val getItems: suspend (page: Int) -> BaseResponse<ConversationMessagesResponse>,
    private val cacheTimeoutMillis: Int = 24 * 60 * 60 * 1000
): RemoteMediator<Int, ConversationMessageIO>() {

    override suspend fun initialize(): InitializeAction {
        val timeElapsed = Clock.System.now().toEpochMilliseconds().minus(
            pagingMetaDao.getCreationTime(PagingEntityType.ConversationMessage.name + conversationId) ?: 0
        )

        return if (timeElapsed < cacheTimeoutMillis) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ConversationMessageIO>
    ): MediatorResult {
        var page: Int = when (loadType) {
            LoadType.REFRESH -> {
                getPagingMetaClosestToCurrentPosition(state)?.nextPage ?: initialPage
            }
            LoadType.PREPEND -> {
                val pagingMeta = getPagingMetaForFirstItem(state)
                val prevKey = if((pagingMeta?.currentPage ?: initialPage) > 0) pagingMeta?.currentPage?.minus(1) else null
                prevKey ?: return MediatorResult.Success(endOfPaginationReached = pagingMeta != null)
            }
            LoadType.APPEND -> {
                val pagingMeta = getPagingMetaForLastItem(state)
                pagingMeta?.nextPage ?: return MediatorResult.Success(endOfPaginationReached = pagingMeta != null)
            }
        }

        try {
            val apiResponse = getItems.invoke(if(loadType == LoadType.REFRESH) initialPage else page)

            val items = apiResponse.success?.data?.content
            val endOfPaginationReached = items?.size != size

            return withContext(Dispatchers.IO) {
                if (loadType == LoadType.REFRESH) {
                    page = initialPage
                    pagingMetaDao.removeAll()
                    //conversationMessageDao.removeAll(conversationId)
                }
                val prevKey = if (page > 1) page - 1 else null
                val nextKey = if (endOfPaginationReached) null else page + 1
                items?.map {
                    PagingMetaIO(
                        entityId = conversationId,
                        previousPage = prevKey,
                        entityType = PagingEntityType.ConversationMessage.name + conversationId,
                        currentPage = page,
                        nextPage = nextKey
                    )
                }?.let {
                    pagingMetaDao.insertAll(it)
                    conversationMessageDao.insertAll(items.onEach { item ->
                        item.conversationId = conversationId
                    })
                    invalidatePagingSource()
                }
                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    /** Returns paging meta data from the current position item */
    private suspend fun getPagingMetaClosestToCurrentPosition(state: PagingState<Int, ConversationMessageIO>): PagingMetaIO? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.conversationId?.let { id ->
                pagingMetaDao.getByEntityId(id)
            }
        }
    }

    /** Returns paging meta data from the first item */
    private suspend fun getPagingMetaForFirstItem(state: PagingState<Int, ConversationMessageIO>): PagingMetaIO? {
        return state.pages.firstOrNull {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.conversationId?.let { id ->
            pagingMetaDao.getByEntityId(id)
        }
    }

    /** Returns paging meta data from the last item */
    private suspend fun getPagingMetaForLastItem(state: PagingState<Int, ConversationMessageIO>): PagingMetaIO? {
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.conversationId?.let { id ->
            pagingMetaDao.getByEntityId(id)
        }
    }
}
