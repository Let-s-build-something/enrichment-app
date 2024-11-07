package ui.network.received

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.network.CirclingActionRequest
import data.io.social.network.CirclingRequest
import data.io.social.network.CirclingRequestsResponse
import data.shared.setPaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class NetworkReceivedRepository(private val httpClient: HttpClient) {

    /** Returns list of network requests */
    private suspend fun getRequests(page: Int, size: Int): BaseResponse<CirclingRequestsResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.get(
                urlString = "/api/v1/social/network/requests",
                block =  {
                    setPaging(
                        size = size,
                        page = page
                    )
                }
            ).getResponse<CirclingRequestsResponse>().also {
                println(it.toString())
            }

            /*if(page < 3) {
                BaseResponse.Success(CirclingRequestsResponse(
                    content = demoData.subList(page * size, ((page + 1) * size) - 1),
                    pagination = PaginationInfo(
                        page = page,
                        size = size,
                        totalPages = 2
                    )
                ))
            }else BaseResponse.Error()*/
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
    suspend fun acceptRequest(action: CirclingActionRequest): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.patch(
                urlString = "/api/v1/social/network/requests/${action.uid}",
                block =  {
                    setBody(action)
                }
            ).getResponse<Any>().also {
                println(it.toString())
            }
        }
    }

    private val demoData = listOf(
        CirclingRequest(displayName = "John Doe 1", photoUrl = "https://picsum.photos/100", tag = "2098d6", uid = "2098d6"),
        CirclingRequest(displayName = "Peter Pan 2", photoUrl = "https://picsum.photos/101", tag = "ae8880", uid = "ae8880"),
        CirclingRequest(displayName = "John Doe 3", photoUrl = "https://picsum.photos/102", tag = "45dd5d", uid = "45dd5d"),
        CirclingRequest(displayName = "Peter Pan 4", photoUrl = "https://picsum.photos/103", tag = "30e76a", uid = "30e76a"),
        CirclingRequest(displayName = "John Doe 5", photoUrl = "https://picsum.photos/104", tag = "7e3531", uid = "7e3531"),
        CirclingRequest(displayName = "Peter Pan 6", photoUrl = "https://picsum.photos/105", tag = "7b8557", uid = "7b8557"),
        CirclingRequest(displayName = "John Doe 7", photoUrl = "https://picsum.photos/106", tag = "7cdf84", uid = "7cdf84"),
        CirclingRequest(displayName = "Peter Pan 8", photoUrl = "https://picsum.photos/107", tag = "553ef0", uid = "553ef0"),
        CirclingRequest(displayName = "John Doe 9", photoUrl = "https://picsum.photos/108", tag = "2cf172", uid = "2cf172"),
        CirclingRequest(displayName = "Peter Pan 10", photoUrl = "https://picsum.photos/109", tag = "bcbc1e", uid = "bcbc1e"),
        CirclingRequest(displayName = "John Doe 11", photoUrl = "https://picsum.photos/110", tag = "f0f2d6", uid = "f0f2d6"),
        CirclingRequest(displayName = "Peter Pan 12", photoUrl = "https://picsum.photos/111", tag = "40099c", uid = "40099c"),
        CirclingRequest(displayName = "John Doe 13", photoUrl = "https://picsum.photos/112", tag = "aac355", uid = "aac355"),
        CirclingRequest(displayName = "Peter Pan 14", photoUrl = "https://picsum.photos/113", tag = "8b1046", uid = "8b1046"),
        CirclingRequest(displayName = "John Doe 15", photoUrl = "https://picsum.photos/114", tag = "aa5711", uid = "aa5711"),
        CirclingRequest(displayName = "Peter Pan 16", photoUrl = "https://picsum.photos/115", tag = "e53008", uid = "e53008"),
        CirclingRequest(displayName = "John Doe 17", photoUrl = "https://picsum.photos/116", tag = "3519eb", uid = "3519eb"),
        CirclingRequest(displayName = "Peter Pan 18", photoUrl = "https://picsum.photos/117", tag = "5021bb", uid = "5021bb"),
        CirclingRequest(displayName = "John Doe 19", photoUrl = "https://picsum.photos/118", tag = "86ac59", uid = "86ac59"),
        CirclingRequest(displayName = "Peter Pan 20", photoUrl = "https://picsum.photos/119", tag = "421554", uid = "421554"),
        CirclingRequest(displayName = "John Doe 21", photoUrl = "https://picsum.photos/120", tag = "ae470e", uid = "ae470e"),
        CirclingRequest(displayName = "Peter Pan 22", photoUrl = "https://picsum.photos/121", tag = "563936", uid = "563936"),
        CirclingRequest(displayName = "John Doe 23", photoUrl = "https://picsum.photos/122", tag = "c25304", uid = "c25304"),
        CirclingRequest(displayName = "Peter Pan 24", photoUrl = "https://picsum.photos/123", tag = "bb7a2c", uid = "bb7a2c"),
        CirclingRequest(displayName = "John Doe 25", photoUrl = "https://picsum.photos/124", tag = "0a347f", uid = "0a347f"),
        CirclingRequest(displayName = "Peter Pan 26", photoUrl = "https://picsum.photos/125", tag = "c7751f", uid = "c7751f"),
        CirclingRequest(displayName = "John Doe 27", photoUrl = "https://picsum.photos/126", tag = "43e028", uid = "43e028"),
        CirclingRequest(displayName = "Peter Pan 28", photoUrl = "https://picsum.photos/127", tag = "0492f6", uid = "0492f6"),
        CirclingRequest(displayName = "John Doe 29", photoUrl = "https://picsum.photos/128", tag = "295dad", uid = "295dad"),
        CirclingRequest(displayName = "Peter Pan 30", photoUrl = "https://picsum.photos/129", tag = "837e55", uid = "837e55"),
        CirclingRequest(displayName = "John Doe 31", photoUrl = "https://picsum.photos/130", tag = "316378", uid = "316378"),
        CirclingRequest(displayName = "Peter Pan 32", photoUrl = "https://picsum.photos/131", tag = "c5fb82", uid = "c5fb82"),
        CirclingRequest(displayName = "John Doe 33", photoUrl = "https://picsum.photos/132", tag = "c1b3c3", uid = "c1b3c3"),
        CirclingRequest(displayName = "Peter Pan 34", photoUrl = "https://picsum.photos/133", tag = "3d6942", uid = "3d6942"),
        CirclingRequest(displayName = "John Doe 35", photoUrl = "https://picsum.photos/134", tag = "40cbca", uid = "40cbca"),
        CirclingRequest(displayName = "Peter Pan 36", photoUrl = "https://picsum.photos/135", tag = "2e4ee8", uid = "2e4ee8"),
        CirclingRequest(displayName = "John Doe 37", photoUrl = "https://picsum.photos/136", tag = "9b84cc", uid = "9b84cc"),
        CirclingRequest(displayName = "Peter Pan 38", photoUrl = "https://picsum.photos/137", tag = "01bb31", uid = "01bb31"),
        CirclingRequest(displayName = "John Doe 39", photoUrl = "https://picsum.photos/138", tag = "644da4", uid = "644da4"),
        CirclingRequest(displayName = "Peter Pan 40", photoUrl = "https://picsum.photos/139", tag = "3b806c", uid = "3b806c"),
        CirclingRequest(displayName = "John Doe 41", photoUrl = "https://picsum.photos/140", tag = "3dbecc", uid = "3dbecc")
    )
}