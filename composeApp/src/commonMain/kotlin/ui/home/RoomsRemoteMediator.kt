package ui.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.base.MatrixPagingMetaIO
import data.io.base.PagingEntityType
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.SyncResponse
import database.dao.ConversationRoomDao
import database.dao.MatrixPagingMetaDao
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.io.IOException
import ui.home.HomeRepository.Companion.INITIAL_BATCH

/**
 * Mediator for reusing locally loaded data and fetching new data from the network if necessary
 */
@OptIn(ExperimentalPagingApi::class)
class RoomsRemoteMediator(
    private val conversationRoomDao: ConversationRoomDao,
    private val pagingMetaDao: MatrixPagingMetaDao,
    private val invalidatePagingSource: () -> Unit,
    private val getItems: suspend (batch: String?) -> BaseResponse<SyncResponse>,
    private val cacheTimeoutMillis: Int = 24 * 60 * 60 * 1000
): RemoteMediator<String, ConversationRoomIO>() {

    private val currentUserUid: String?
        get() = Firebase.auth.currentUser?.uid

    override suspend fun initialize(): InitializeAction {
        val timeElapsed = Clock.System.now().toEpochMilliseconds().minus(
            pagingMetaDao.getCreationTime(PagingEntityType.ConversationRoom.name) ?: 0
        )

        return if (timeElapsed < cacheTimeoutMillis) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<String, ConversationRoomIO>
    ): MediatorResult {
        var batch: String? = when (loadType) {
            LoadType.REFRESH -> {
                getPagingMetaClosestToCurrentPosition(state)?.nextBatch
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(
                    endOfPaginationReached = getPagingMetaForFirstItem(state) != null
                )
            }
            LoadType.APPEND -> {
                val pagingMeta = getPagingMetaForLastItem(state)
                pagingMeta?.nextBatch ?: return MediatorResult.Success(
                    endOfPaginationReached = pagingMeta != null
                )
            }
        }

        try {
            val apiResponse = getItems.invoke(if(loadType == LoadType.REFRESH) null else batch)

            val items = apiResponse.success?.data?.rooms?.let { response ->
                mutableListOf<ConversationRoomIO>().apply {
                    addAll(response.join.values)
                    addAll(response.invite.values)
                    addAll(response.knock.values)
                    addAll(response.leave.values)
                }
            }
            val endOfPaginationReached = apiResponse.success?.data?.nextBatch == null

            return withContext(Dispatchers.IO) {
                if (loadType == LoadType.REFRESH) {
                    batch = null
                    pagingMetaDao.removeAll()
                    //conversationRoomDao.removeAll()
                }
                items?.map { item ->
                    MatrixPagingMetaIO(
                        entityId = item.id,
                        nextBatch = apiResponse.success?.data?.nextBatch,
                        entityType = PagingEntityType.ConversationRoom,
                        batch = batch ?: INITIAL_BATCH
                    )
                }?.let {
                    pagingMetaDao.insertAll(it)
                    conversationRoomDao.insertAll(items.onEach { room ->
                        room.batch = batch ?: INITIAL_BATCH
                        room.nextBatch = apiResponse.success?.data?.nextBatch
                        room.ownerPublicId = currentUserUid
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
    private suspend fun getPagingMetaClosestToCurrentPosition(
        state: PagingState<String, ConversationRoomIO>
    ): MatrixPagingMetaIO? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                pagingMetaDao.getByEntityId(id)
            }
        }
    }

    /** Returns paging meta data from the first item */
    private suspend fun getPagingMetaForFirstItem(
        state: PagingState<String, ConversationRoomIO>
    ): MatrixPagingMetaIO? {
        return state.pages.firstOrNull {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.id?.let { id ->
            pagingMetaDao.getByEntityId(id)
        }
    }

    /** Returns paging meta data from the last item */
    private suspend fun getPagingMetaForLastItem(
        state: PagingState<String, ConversationRoomIO>
    ): MatrixPagingMetaIO? {
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.id?.let { id ->
            pagingMetaDao.getByEntityId(id)
        }
    }
}
