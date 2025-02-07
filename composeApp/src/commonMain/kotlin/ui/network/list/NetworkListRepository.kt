@file:OptIn(ExperimentalUuidApi::class)

package ui.network.list

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.base.paging.PaginationInfo
import data.io.social.network.request.NetworkListResponse
import data.io.user.NetworkItemIO
import data.shared.setPaging
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi

/** Class for calling APIs and remote work in general */
class NetworkListRepository(
    private val httpClient: HttpClient,
    private val networkItemDao: NetworkItemDao,
    private val pagingMetaDao: PagingMetaDao
) {

    /** returns a list of network list */
    private suspend fun getNetworkList(page: Int, size: Int): BaseResponse<NetworkListResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<NetworkListResponse> {
                get(
                    urlString = "/api/v1/social/network/users",
                    block = {
                        setPaging(
                            size = size,
                            page = page
                        )
                    }
                )
            }
        }
    }

    /** Returns a flow of network list */
    @OptIn(ExperimentalPagingApi::class)
    fun getNetworkListFlow(
        config: PagingConfig,
        ownerPublicId: () -> String?
    ): Pager<Int, NetworkItemIO> {
        val scope = CoroutineScope(Dispatchers.Default)
        var currentPagingSource: NetworkRoomSource? = null

        return Pager(
            config = config,
            pagingSourceFactory = {
                NetworkRoomSource(
                    getItems = { page ->
                        val res = networkItemDao.getPaginated(
                            ownerPublicId = ownerPublicId(),
                            limit = config.pageSize,
                            offset = page * config.pageSize
                        )

                        BaseResponse.Success(
                            NetworkListResponse(
                                content = res,
                                pagination = PaginationInfo(
                                    page = page,
                                    size = res.size,
                                    totalItems = networkItemDao.getCount(ownerPublicId())
                                )
                            )
                        )
                    },
                    size = config.pageSize
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            },
            remoteMediator = NetworkRemoteMediator(
                networkItemDao = networkItemDao,
                pagingMetaDao = pagingMetaDao,
                size = config.pageSize,
                getItems = ::getNetworkList,
                invalidatePagingSource = {
                    scope.coroutineContext.cancelChildren()
                    scope.launch {
                        delay(200)
                        currentPagingSource?.invalidate()
                    }
                }
            )
        )
    }
}
