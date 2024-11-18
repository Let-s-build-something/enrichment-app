package ui.network.list

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.social.network.request.NetworkListResponse
import data.io.user.NetworkItemIO
import data.shared.setPaging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Class for calling APIs and remote work in general */
class NetworkListRepository(private val httpClient: HttpClient) {

    /** returns a list of network list */
    private suspend fun getNetworkList(page: Int, size: Int): BaseResponse<NetworkListResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<NetworkListResponse> {
                get(
                    urlString = "/api/v1/social/network/users",
                    block =  {
                        setPaging(
                            size = size,
                            page = page
                        )
                    }
                )
            }

            /*if(page <= proximityDemoData.size/size) {
                BaseResponse.Success(NetworkListResponse(
                    content = proximityDemoData.subList(page * size, ((page + 1) * size) - 1),
                    pagination = PaginationInfo(
                        page = page,
                        size = size,
                        totalPages = proximityDemoData.size/size
                    )
                ))
            }else BaseResponse.Error()*/
        }
    }

    /** Returns a flow of network list */
    fun getNetworkListFlow(config: PagingConfig): Pager<Int, NetworkItemIO> {
        return Pager(config) {
            NetworkListSource(
                getRequests = ::getNetworkList,
                size = config.pageSize
            )
        }
    }

    internal companion object {

        internal val family = listOf(
            NetworkItemIO(proximity = 10.1f, displayName = "Dad", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "20l98d6"),
            NetworkItemIO(proximity = 10.7f, displayName = "Mom", photoUrl = "https://picsum.photos/101", tag = "2098d6", publicId = "2098d6d"),
            NetworkItemIO(proximity = 10.9f, displayName = "Sister", photoUrl = "https://picsum.photos/102", tag = "2098d6", publicId = "2098dc6d"),
            NetworkItemIO(proximity = 10.4f, displayName = "Brother", photoUrl = "https://picsum.photos/103", tag = "2098d6", publicId = "2098db6d"),
            NetworkItemIO(proximity = 10.9f, displayName = "Son", photoUrl = "https://picsum.photos/104", tag = "2098d6", publicId = "2098d6ed"),
            NetworkItemIO(proximity = 10.2f, displayName = "Grandma", photoUrl = "https://picsum.photos/105", tag = "2098d6", publicId = "2098dg6d"),
            NetworkItemIO(proximity = 10.1f, displayName = "Grandpa", photoUrl = "https://picsum.photos/106", tag = "2098d6", publicId = "2098d6sd")
        )

        private val friends = listOf(
            NetworkItemIO(proximity = 9.9f, displayName = "Jack", photoUrl = "https://picsum.photos/107", tag = "2098d6", publicId = "20f98d6"),
            NetworkItemIO(proximity = 9.3f, displayName = "Peter", photoUrl = "https://picsum.photos/108", tag = "2098d6", publicId = "2098df6dl"),
            NetworkItemIO(proximity = 9.2f, displayName = "James", photoUrl = "https://picsum.photos/109", tag = "2098d6", publicId = "20l98fdc6d"),
            NetworkItemIO(proximity = 9.6f, displayName = "Mark", photoUrl = "https://picsum.photos/110", tag = "2098d6", publicId = "20f98dbl6d"),
            NetworkItemIO(proximity = 9.8f, displayName = "Carl", photoUrl = "https://picsum.photos/111", tag = "2098d6", publicId = "209l8d6efd"),
            NetworkItemIO(proximity = 9.1f, displayName = "Arnold", photoUrl = "https://picsum.photos/112", tag = "2098d6", publicId = "2098ldfg6d"),
        )

        private val acquaintances = listOf(
            NetworkItemIO(proximity = 8.5f, displayName = "Jack", photoUrl = "https://picsum.photos/113", tag = "2098d6", publicId = "2098sd6"),
            NetworkItemIO(proximity = 8.3f, displayName = "Peter", photoUrl = "https://picsum.photos/114", tag = "2098d6", publicId = "209s8d6dl"),
            NetworkItemIO(proximity = 8.77f, displayName = "James", photoUrl = "https://picsum.photos/115", tag = "2098d6", publicId = "s20l98dc6d"),
            NetworkItemIO(proximity = 8.7f, displayName = "Mark", photoUrl = "https://picsum.photos/116", tag = "2098d6", publicId = "20s98dbl6d"),
            NetworkItemIO(proximity = 8.8f, displayName = "Carl", photoUrl = "https://picsum.photos/117", tag = "2098d6", publicId = "209l8d6eds"),
            NetworkItemIO(proximity = 8.2f, displayName = "Arnold", photoUrl = "https://picsum.photos/118", tag = "2098d6", publicId = "2098ldg6sd"),
        )

        private val demoData = listOf(
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 1", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 2", photoUrl = "https://picsum.photos/101", tag = "ae8880", publicId = "ae8880"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 3", photoUrl = "https://picsum.photos/102", tag = "45dd5d", publicId = "45dd5d"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 4", photoUrl = "https://picsum.photos/103", tag = "30e76a", publicId = "30e76a"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 5", photoUrl = "https://picsum.photos/104", tag = "7e3531", publicId = "7e3531"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 6", photoUrl = "https://picsum.photos/105", tag = "7b8557", publicId = "7b8557"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 7", photoUrl = "https://picsum.photos/106", tag = "7cdf84", publicId = "7cdf84"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 8", photoUrl = "https://picsum.photos/107", tag = "553ef0", publicId = "553ef0"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 9", photoUrl = "https://picsum.photos/108", tag = "2cf172", publicId = "2cf172"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 10", photoUrl = "https://picsum.photos/109", tag = "bcbc1e", publicId = "bcbc1e"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 11", photoUrl = "https://picsum.photos/110", tag = "f0f2d6", publicId = "f0f2d6"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 12", photoUrl = "https://picsum.photos/111", tag = "40099c", publicId = "40099c"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 13", photoUrl = "https://picsum.photos/112", tag = "aac355", publicId = "aac355"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 14", photoUrl = "https://picsum.photos/113", tag = "8b1046", publicId = "8b1046"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 15", photoUrl = "https://picsum.photos/114", tag = "aa5711", publicId = "aa5711"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 16", photoUrl = "https://picsum.photos/115", tag = "e53008", publicId = "e53008"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 17", photoUrl = "https://picsum.photos/116", tag = "3519eb", publicId = "3519eb"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 18", photoUrl = "https://picsum.photos/117", tag = "5021bb", publicId = "5021bb"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 19", photoUrl = "https://picsum.photos/118", tag = "86ac59", publicId = "86ac59"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 20", photoUrl = "https://picsum.photos/119", tag = "421554", publicId = "421554"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 21", photoUrl = "https://picsum.photos/120", tag = "ae470e", publicId = "ae470e"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 22", photoUrl = "https://picsum.photos/121", tag = "563936", publicId = "563936"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 23", photoUrl = "https://picsum.photos/122", tag = "c25304", publicId = "c25304"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 24", photoUrl = "https://picsum.photos/123", tag = "bb7a2c", publicId = "bb7a2c"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 25", photoUrl = "https://picsum.photos/124", tag = "0a347f", publicId = "0a347f"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 26", photoUrl = "https://picsum.photos/125", tag = "c7751f", publicId = "c7751f"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 27", photoUrl = "https://picsum.photos/126", tag = "43e028", publicId = "43e028"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 28", photoUrl = "https://picsum.photos/127", tag = "0492f6", publicId = "0492f6"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 29", photoUrl = "https://picsum.photos/128", tag = "295dad", publicId = "295dad"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 30", photoUrl = "https://picsum.photos/129", tag = "837e55", publicId = "837e55"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 31", photoUrl = "https://picsum.photos/130", tag = "316378", publicId = "316378"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 32", photoUrl = "https://picsum.photos/131", tag = "c5fb82", publicId = "c5fb82"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 33", photoUrl = "https://picsum.photos/132", tag = "c1b3c3", publicId = "c1b3c3"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 34", photoUrl = "https://picsum.photos/133", tag = "3d6942", publicId = "3d6942"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 35", photoUrl = "https://picsum.photos/134", tag = "40cbca", publicId = "40cbca"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 36", photoUrl = "https://picsum.photos/135", tag = "2e4ee8", publicId = "2e4ee8"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 37", photoUrl = "https://picsum.photos/136", tag = "9b84cc", publicId = "9b84cc"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 38", photoUrl = "https://picsum.photos/137", tag = "01bb31", publicId = "01bb31"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 39", photoUrl = "https://picsum.photos/138", tag = "644da4", publicId = "644da4"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "Peter Pan 40", photoUrl = "https://picsum.photos/139", tag = "3b806c", publicId = "3b806c"),
            NetworkItemIO(proximity = (10..30).random().div(10f), displayName = "John Doe 41", photoUrl = "https://picsum.photos/140", tag = "3dbecc", publicId = "3dbecc"),
        )

        @OptIn(ExperimentalUuidApi::class)
        private val community = demoData.map { it.copy(proximity = (40..70).random().div(10f), publicId = Uuid.random().toString()) }

        private val strangers = demoData

        val proximityDemoData = (family + friends + acquaintances + community + strangers).sortedByDescending { it.proximity }
    }
}