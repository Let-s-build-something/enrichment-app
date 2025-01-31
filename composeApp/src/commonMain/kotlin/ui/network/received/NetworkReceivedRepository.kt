package ui.network.received

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.social.network.request.CirclingActionRequest
import data.io.social.network.request.CirclingRequest
import data.io.social.network.request.CirclingRequestsResponse
import data.shared.setPaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class NetworkReceivedRepository(private val httpClient: HttpClient) {

    /** Returns list of network requests */
    private suspend fun getRequests(page: Int, size: Int): BaseResponse<CirclingRequestsResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<CirclingRequestsResponse> {
                get(
                    urlString = "/api/v1/social/network/requests",
                    block =  {
                        setPaging(
                            size = size,
                            page = page
                        )
                    }
                )
            }
        }
    }

    /** Returns a flow of requests */
    fun getRequestsFlow(config: PagingConfig): Pager<Int, CirclingRequest> {
        return Pager(config) {
            NetworkReceivedSource(
                getRequests = ::getRequests,
                size = config.pageSize
            )
        }
    }

    /** Acts upon a circling request */
    suspend fun acceptRequest(
        publicId: String,
        proximity: Float?
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<Any> {
                patch(
                    urlString = "/api/v1/social/network/requests/${publicId}",
                    block =  {
                        setBody(CirclingActionRequest(proximity = proximity))
                    }
                )
            }
        }
    }
}