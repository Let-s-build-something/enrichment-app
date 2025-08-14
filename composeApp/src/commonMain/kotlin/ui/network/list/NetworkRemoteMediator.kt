package ui.network.list

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import coil3.network.HttpException
import data.io.base.BaseResponse
import data.io.base.paging.PagingEntityType
import data.io.base.paging.PagingMetaIO
import data.io.social.network.request.NetworkListResponse
import data.io.user.NetworkItemIO
import data.shared.SharedDataManager
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.io.IOException
import org.koin.mp.KoinPlatform

/**
 * Mediator for reusing locally loaded data and fetching new data from the network if necessary
 */
@OptIn(ExperimentalPagingApi::class)
class NetworkRemoteMediator (
    private val networkItemDao: NetworkItemDao,
    private val pagingMetaDao: PagingMetaDao,
    private val size: Int,
    private val initialPage: Int = 0,
    private val invalidatePagingSource: () -> Unit,
    private val getItems: suspend (page: Int, size: Int) -> BaseResponse<NetworkListResponse>,
    private val cacheTimeoutMillis: Int = 24 * 60 * 60 * 1000
): RemoteMediator<Int, NetworkItemIO>() {

    private val dataManager: SharedDataManager by KoinPlatform.getKoin().inject()
    private val ownerUserPublicId: String?
        get() = dataManager.currentUser.value?.userId

    override suspend fun initialize(): InitializeAction {
        val timeElapsed = Clock.System.now().toEpochMilliseconds().minus(
            pagingMetaDao.getCreationTime(PagingEntityType.NetworkItem.name) ?: 0
        )

        return if (timeElapsed < cacheTimeoutMillis) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NetworkItemIO>
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
            val apiResponse = getItems.invoke(
                if(loadType == LoadType.REFRESH) initialPage else page,
                size
            )

            val items = apiResponse.success?.data?.content
            val endOfPaginationReached = items?.size != size

            return withContext(Dispatchers.IO) {
                if (loadType == LoadType.REFRESH) {
                    page = initialPage
                    pagingMetaDao.removeAll()
                    //networkItemDao.removeAll()
                }
                val prevKey = if (page > 1) page - 1 else null
                val nextKey = if (endOfPaginationReached) null else page + 1
                items?.map {
                    PagingMetaIO(
                        entityId = "${ownerUserPublicId}_${it.userPublicId}",
                        previousPage = prevKey,
                        currentPage = page,
                        nextPage = nextKey
                    )
                }?.let {
                    pagingMetaDao.insertAll(it)
                    networkItemDao.insertAll(
                        items.map { item ->
                            item.copy(ownerUserId = ownerUserPublicId)
                        }
                    )
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
    private suspend fun getPagingMetaClosestToCurrentPosition(state: PagingState<Int, NetworkItemIO>): PagingMetaIO? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.userPublicId?.let { id ->
                pagingMetaDao.getByEntityId("${ownerUserPublicId}_$id")
            }
        }
    }

    /** Returns paging meta data from the first item */
    private suspend fun getPagingMetaForFirstItem(state: PagingState<Int, NetworkItemIO>): PagingMetaIO? {
        return state.pages.firstOrNull {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.userPublicId?.let { id ->
            pagingMetaDao.getByEntityId("${ownerUserPublicId}_$id")
        }
    }

    /** Returns paging meta data from the last item */
    private suspend fun getPagingMetaForLastItem(state: PagingState<Int, NetworkItemIO>): PagingMetaIO? {
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.userPublicId?.let { id ->
            pagingMetaDao.getByEntityId("${ownerUserPublicId}_$id")
        }
    }
}
