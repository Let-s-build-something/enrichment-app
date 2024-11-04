package ui.network.list

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.network.CirclingRequest
import data.io.social.network.CirclingRequestsResponse
import data.shared.setPaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class NetworkListRepository(private val httpClient: HttpClient) {

    /** returns a list of network list */
    private suspend fun getNetworkList(page: Int, size: Int): BaseResponse<CirclingRequestsResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.get(
                urlString = "/v1/social/network",
                block =  {
                    setPaging(
                        size = size,
                        page = page
                    )
                }
            ).getResponse<CirclingRequestsResponse>().also {
                println(it.toString())
            }
        }
    }

    /** Returns a flow of network list */
    fun getNetworkListFlow(config: PagingConfig): Pager<Int, CirclingRequest> {
        return Pager(config) {
            NetworkListSource(
                getRequests = ::getNetworkList,
                size = config.pageSize
            )
        }
    }
}