package ui.conversation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingEntityType
import data.io.matrix.room.event.content.constructMessages
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.ConversationMessagesResponse
import database.dao.ConversationMessageDao
import database.dao.matrix.MatrixPagingMetaDao
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
    private val pagingMetaDao: MatrixPagingMetaDao,
    private val conversationId: String,
    private val size: Int,
    private val initialBatch: suspend () -> String?,
    private val prevBatch: suspend () -> String?,
    private val invalidatePagingSource: () -> Unit,
    private val getItems: suspend (batch: String?) -> BaseResponse<ConversationMessagesResponse>,
    private val cacheTimeoutMillis: Int = 24 * 60 * 60 * 1000
): RemoteMediator<String, ConversationMessageIO>() {

    val entityType: String
        get() = "${PagingEntityType.ConversationMessage}_$conversationId"

    override suspend fun initialize(): InitializeAction {
        val timeElapsed = Clock.System.now().toEpochMilliseconds().minus(
            pagingMetaDao.getCreationTime(entityType) ?: 0
        )

        return if (timeElapsed < cacheTimeoutMillis) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<String, ConversationMessageIO>
    ): MediatorResult {
        var batch: String? = when (loadType) {
            LoadType.REFRESH -> getPagingMetaClosestToCurrentPosition(state)?.currentBatch
            LoadType.PREPEND -> getPagingMetaForFirstItem(state)?.prevBatch ?: prevBatch()
            LoadType.APPEND -> getPagingMetaForLastItem(state)?.nextBatch
        }

        try {
            val data = getItems.invoke(batch).success?.data

            val items = constructMessages(
                state = data?.state.orEmpty(),
                timeline = data?.chunk.orEmpty(),
                roomId = conversationId,
                prevBatch = data?.end,
                nextBatch = data?.start,
                currentBatch = batch
            )
            println("kostka_test, saving ${items.size} messages under $batch")
            val endOfPaginationReached = items.size != size

            return withContext(Dispatchers.IO) {
                if (loadType == LoadType.REFRESH) pagingMetaDao.removeAll()

                items.map {
                    MatrixPagingMetaIO(
                        entityId = it.id,
                        prevBatch = data?.end,
                        nextBatch = data?.start,
                        currentBatch = batch,
                        entityType = entityType
                    )
                }.also {
                    pagingMetaDao.insertAll(it)
                }
                // TODO the reason why data appeared and disappeared right after is because last batch will be null,
                //  yet the first one as well, we should identify them in a different way
                // first data, we finally get to know the current batch with a hack of sorts
                println("kostka_test, batch: $batch, initial: ${initialBatch()}, updating to ${data?.start}")
                if(batch == initialBatch()) {
                    conversationMessageDao.setInitialBatch(
                        conversationId = conversationId,
                        batch = data?.start
                    )
                }
                conversationMessageDao.insertAll(items)
                invalidatePagingSource()
                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    /** Returns paging meta data from the current position item */
    private suspend fun getPagingMetaClosestToCurrentPosition(state: PagingState<String, ConversationMessageIO>): MatrixPagingMetaIO? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                pagingMetaDao.getByEntityId(id)
            }
        }
    }

    /** Returns paging meta data from the first item */
    private suspend fun getPagingMetaForFirstItem(state: PagingState<String, ConversationMessageIO>): MatrixPagingMetaIO? {
        return state.pages.firstOrNull {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.id?.let { id ->
            pagingMetaDao.getByEntityId(id)
        }
    }

    /** Returns paging meta data from the last item */
    private suspend fun getPagingMetaForLastItem(state: PagingState<String, ConversationMessageIO>): MatrixPagingMetaIO? {
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.id?.let { id ->
            pagingMetaDao.getByEntityId(id)
        }
    }
}
