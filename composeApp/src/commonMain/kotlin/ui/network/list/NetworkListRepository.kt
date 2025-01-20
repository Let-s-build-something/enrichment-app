@file:OptIn(ExperimentalUuidApi::class)

package ui.network.list

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.base.PaginationInfo
import data.io.social.network.request.NetworkListResponse
import data.io.user.NetworkItemIO
import data.shared.setPaging
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
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
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

            // TODO remove DEMO data once it's not needed
            if (page <= proximityDemoData.size / size) {
                BaseResponse.Success(
                    NetworkListResponse(
                        content = proximityDemoData.subList(
                            page * size,
                            kotlin.math.min(
                                (page + 1) * size,
                                page * size + (proximityDemoData.size - page * size)
                            ).coerceAtMost(proximityDemoData.size)
                        ),
                        pagination = PaginationInfo(
                            page = page,
                            size = size,
                            totalPages = (proximityDemoData.size / size.toFloat()).roundToInt()
                        )
                    )
                )
            } else BaseResponse.Error()
        }
    }

    /** Returns a flow of network list */
    @OptIn(ExperimentalPagingApi::class)
    fun getNetworkListFlow(config: PagingConfig): Pager<Int, NetworkItemIO> {
        val scope = CoroutineScope(Dispatchers.Default)
        var currentPagingSource: NetworkRoomSource? = null

        return Pager(
            config = config,
            pagingSourceFactory = {
                NetworkRoomSource(
                    getItems = { page ->
                        val ownerId = Firebase.auth.currentUser?.uid
                        val res = networkItemDao.getPaginated(
                            ownerPublicId = ownerId,
                            limit = config.pageSize,
                            offset = page * config.pageSize
                        )

                        BaseResponse.Success(
                            NetworkListResponse(
                                content = res,
                                pagination = PaginationInfo(
                                    page = page,
                                    size = res.size,
                                    totalItems = networkItemDao.getCount(ownerId)
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

    internal companion object {

        private val family = listOf(
            NetworkItemIO(proximity = 10.9f, name = "Sister", photoUrl = "https://picsum.photos/102", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 10.8f, name = "Son", photoUrl = "https://picsum.photos/104", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 10.7f, name = "Mom", photoUrl = "https://picsum.photos/101", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 10.4f, name = "Brother", photoUrl = "https://picsum.photos/103", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 10.2f, name = "Grandma", photoUrl = "https://picsum.photos/105", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 10.15f, name = "Dad", photoUrl = "https://picsum.photos/100", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 10.1f, name = "Grandpa", photoUrl = "https://picsum.photos/106", tag = "2098d6", userPublicId = Uuid.random().toString())
        )

        private val friends = listOf(
            NetworkItemIO(proximity = 9.9f, name = "Jack", photoUrl = "https://picsum.photos/107", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 9.3f, name = "Peter", photoUrl = "https://picsum.photos/108", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 9.2f, name = "James", photoUrl = "https://picsum.photos/109", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 9.6f, name = "Mark", photoUrl = "https://picsum.photos/110", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 9.8f, name = "Carl", photoUrl = "https://picsum.photos/111", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 9.1f, name = "Arnold", photoUrl = "https://picsum.photos/112", tag = "2098d6", userPublicId = Uuid.random().toString()),
        )

        private val acquaintances = listOf(
            NetworkItemIO(proximity = 8.5f, name = "Jack", photoUrl = "https://picsum.photos/113", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.3f, name = "Peter", photoUrl = "https://picsum.photos/114", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.77f, name = "James", photoUrl = "https://picsum.photos/115", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.7f, name = "Mark", photoUrl = "https://picsum.photos/116", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.8f, name = "Carl", photoUrl = "https://picsum.photos/117", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.2f, name = "Arnold", photoUrl = "https://picsum.photos/118", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.9f, name = "Helen", photoUrl = "https://picsum.photos/119", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.4f, name = "Linda", photoUrl = "https://picsum.photos/120", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.6f, name = "Susan", photoUrl = "https://picsum.photos/121", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.75f, name = "Betty", photoUrl = "https://picsum.photos/122", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.65f, name = "Nancy", photoUrl = "https://picsum.photos/123", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.45f, name = "George", photoUrl = "https://picsum.photos/124", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.5f, name = "Paul", photoUrl = "https://picsum.photos/125", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.55f, name = "Ruth", photoUrl = "https://picsum.photos/126", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.6f, name = "Tom", photoUrl = "https://picsum.photos/127", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.68f, name = "Eve", photoUrl = "https://picsum.photos/128", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.58f, name = "Chris", photoUrl = "https://picsum.photos/129", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.52f, name = "Steve", photoUrl = "https://picsum.photos/130", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.9f, name = "Sarah", photoUrl = "https://picsum.photos/131", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.78f, name = "Laura", photoUrl = "https://picsum.photos/132", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.72f, name = "Michael", photoUrl = "https://picsum.photos/133", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.76f, name = "Jessica", photoUrl = "https://picsum.photos/134", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.64f, name = "Daniel", photoUrl = "https://picsum.photos/135", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.66f, name = "Emma", photoUrl = "https://picsum.photos/136", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.8f, name = "Olivia", photoUrl = "https://picsum.photos/137", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.5f, name = "Liam", photoUrl = "https://picsum.photos/138", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.63f, name = "Sophia", photoUrl = "https://picsum.photos/139", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.53f, name = "Alexander", photoUrl = "https://picsum.photos/140", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.88f, name = "Isabella", photoUrl = "https://picsum.photos/141", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.7f, name = "Elijah", photoUrl = "https://picsum.photos/142", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.8f, name = "Mason", photoUrl = "https://picsum.photos/143", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.68f, name = "Logan", photoUrl = "https://picsum.photos/144", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.67f, name = "Lucas", photoUrl = "https://picsum.photos/145", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.7f, name = "Henry", photoUrl = "https://picsum.photos/146", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.65f, name = "Aiden", photoUrl = "https://picsum.photos/147", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.75f, name = "Charlotte", photoUrl = "https://picsum.photos/148", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.62f, name = "Amelia", photoUrl = "https://picsum.photos/149", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.7f, name = "Harper", photoUrl = "https://picsum.photos/150", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.8f, name = "Evelyn", photoUrl = "https://picsum.photos/151", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.78f, name = "Abigail", photoUrl = "https://picsum.photos/152", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.64f, name = "Ella", photoUrl = "https://picsum.photos/153", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.85f, name = "Mia", photoUrl = "https://picsum.photos/154", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.9f, name = "Scarlett", photoUrl = "https://picsum.photos/155", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.72f, name = "Emily", photoUrl = "https://picsum.photos/156", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.69f, name = "Madison", photoUrl = "https://picsum.photos/157", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.58f, name = "Layla", photoUrl = "https://picsum.photos/158", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.77f, name = "Luna", photoUrl = "https://picsum.photos/159", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.66f, name = "Zoe", photoUrl = "https://picsum.photos/160", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.82f, name = "Grace", photoUrl = "https://picsum.photos/161", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.5f, name = "Chloe", photoUrl = "https://picsum.photos/162", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.75f, name = "Avery", photoUrl = "https://picsum.photos/163", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.74f, name = "Mila", photoUrl = "https://picsum.photos/164", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.71f, name = "Aria", photoUrl = "https://picsum.photos/165", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.79f, name = "Isla", photoUrl = "https://picsum.photos/166", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.81f, name = "Ellie", photoUrl = "https://picsum.photos/167", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.7f, name = "Lily", photoUrl = "https://picsum.photos/168", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.6f, name = "Aurora", photoUrl = "https://picsum.photos/169", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = 8.63f, name = "Hazel", photoUrl = "https://picsum.photos/170", tag = "2098d6", userPublicId = Uuid.random().toString())
        )

        private val demoData = listOf(
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 1", photoUrl = "https://picsum.photos/100", tag = "2098d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 2", photoUrl = "https://picsum.photos/101", tag = "ae8880", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 3", photoUrl = "https://picsum.photos/102", tag = "45dd5d", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 4", photoUrl = "https://picsum.photos/103", tag = "30e76a", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 5", photoUrl = "https://picsum.photos/104", tag = "7e3531", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 6", photoUrl = "https://picsum.photos/105", tag = "7b8557", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 7", photoUrl = "https://picsum.photos/106", tag = "7cdf84", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 8", photoUrl = "https://picsum.photos/107", tag = "553ef0", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 9", photoUrl = "https://picsum.photos/108", tag = "2cf172", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 10", photoUrl = "https://picsum.photos/109", tag = "bcbc1e", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 11", photoUrl = "https://picsum.photos/110", tag = "f0f2d6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 12", photoUrl = "https://picsum.photos/111", tag = "40099c", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 13", photoUrl = "https://picsum.photos/112", tag = "aac355", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 14", photoUrl = "https://picsum.photos/113", tag = "8b1046", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 15", photoUrl = "https://picsum.photos/114", tag = "aa5711", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 16", photoUrl = "https://picsum.photos/115", tag = "e53008", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 17", photoUrl = "https://picsum.photos/116", tag = "3519eb", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 18", photoUrl = "https://picsum.photos/117", tag = "5021bb", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 19", photoUrl = "https://picsum.photos/118", tag = "86ac59", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 20", photoUrl = "https://picsum.photos/119", tag = "421554", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 21", photoUrl = "https://picsum.photos/120", tag = "ae470e", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 22", photoUrl = "https://picsum.photos/121", tag = "563936", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 23", photoUrl = "https://picsum.photos/122", tag = "c25304", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 24", photoUrl = "https://picsum.photos/123", tag = "bb7a2c", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 25", photoUrl = "https://picsum.photos/124", tag = "0a347f", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 26", photoUrl = "https://picsum.photos/125", tag = "c7751f", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 27", photoUrl = "https://picsum.photos/126", tag = "43e028", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 28", photoUrl = "https://picsum.photos/127", tag = "0492f6", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 29", photoUrl = "https://picsum.photos/128", tag = "295dad", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 30", photoUrl = "https://picsum.photos/129", tag = "837e55", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 31", photoUrl = "https://picsum.photos/130", tag = "316378", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 32", photoUrl = "https://picsum.photos/131", tag = "c5fb82", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 33", photoUrl = "https://picsum.photos/132", tag = "c1b3c3", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 34", photoUrl = "https://picsum.photos/133", tag = "3d6942", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 35", photoUrl = "https://picsum.photos/134", tag = "40cbca", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 36", photoUrl = "https://picsum.photos/135", tag = "2e4ee8", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 37", photoUrl = "https://picsum.photos/136", tag = "9b84cc", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 38", photoUrl = "https://picsum.photos/137", tag = "01bb31", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 39", photoUrl = "https://picsum.photos/138", tag = "644da4", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "Peter Pan 40", photoUrl = "https://picsum.photos/139", tag = "3b806c", userPublicId = Uuid.random().toString()),
            NetworkItemIO(proximity = (10..30).random().div(10f), name = "John Doe 41", photoUrl = "https://picsum.photos/140", tag = "3dbecc", userPublicId = Uuid.random().toString()),
        )

        @OptIn(ExperimentalUuidApi::class)
        private val community = demoData.map { it.copy(proximity = (40..70).random().div(10f), userPublicId = Uuid.random().toString()) }

        private val strangers = demoData

        val proximityDemoData = (family + friends + acquaintances + community + strangers).sortedByDescending { it.proximity }
    }
}