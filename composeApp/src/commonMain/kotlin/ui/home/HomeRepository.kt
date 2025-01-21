package ui.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.social.network.conversation.matrix.MatrixEventContent
import data.io.social.network.conversation.matrix.RoomNotificationsCount
import data.io.social.network.conversation.matrix.RoomSummary
import data.io.social.network.conversation.matrix.RoomType
import data.io.social.network.conversation.matrix.RoomsResponseIO
import data.io.social.network.request.NetworkListResponse
import data.io.user.matrix.SyncResponse
import database.dao.ConversationRoomDao
import database.dao.MatrixPagingMetaDao
import database.dao.NetworkItemDao
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.login.safeRequest
import ui.network.connection.SocialConnectionUpdate
import ui.network.list.NetworkListRepository.Companion.proximityDemoData

class HomeRepository(
    private val httpClient: HttpClient,
    private val conversationRoomDao: ConversationRoomDao,
    private val networkDao: NetworkItemDao,
    private val pagingMetaDao: MatrixPagingMetaDao
) {
    /** returns a list of network list */
    suspend fun getNetworkItems(): BaseResponse<NetworkListResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<NetworkListResponse> {
                get(urlString = "/api/v1/social/network/users")
            }.takeIf { it.success != null } ?: BaseResponse.Success(NetworkListResponse(content = proximityDemoData))
        }
    }

    /** returns a list of network list */
    private suspend fun getSyncData(batch: String?): BaseResponse<SyncResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<SyncResponse> {
                get(
                    urlString = "/api/v1/conversation/sync",
                    block = {
                        parameters {
                            if(batch != null) set("since", batch)
                        }
                    }
                )
            }.takeIf { it.success?.data != null } ?: BaseResponse.Success(DEMO_SYNC)
        }
    }

    /** Returns a flow of network list */
    @OptIn(ExperimentalPagingApi::class)
    fun getConversationRoomPager(config: PagingConfig): Pager<String, ConversationRoomIO> {
        val scope = CoroutineScope(Dispatchers.Default)
        var currentPagingSource: ConversationRoomSource? = null

        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    size = config.pageSize,
                    getItems = { batch ->
                        val ownerId = Firebase.auth.currentUser?.uid
                        val res = conversationRoomDao.getPaginated(
                            ownerPublicId = ownerId,
                            batch = batch
                        )

                        val join = hashMapOf<String, ConversationRoomIO>()
                        val invite = hashMapOf<String, ConversationRoomIO>()
                        val knock = hashMapOf<String, ConversationRoomIO>()
                        val leave = hashMapOf<String, ConversationRoomIO>()
                        res.forEach {
                            when(it.type) {
                                RoomType.Invited -> invite[it.id] = it
                                RoomType.Joined -> join[it.id] = it
                                RoomType.Knocked -> knock[it.id] = it
                                RoomType.Left -> leave[it.id] = it
                            }
                        }
                        BaseResponse.Success(
                            SyncResponse(
                                nextBatch = res.firstOrNull()?.nextBatch,
                                rooms = RoomsResponseIO(
                                    join = join,
                                    invite = invite,
                                    knock = knock,
                                    leave = leave
                                )
                            )
                        )
                    }
                ).also { pagingSource ->
                    currentPagingSource = pagingSource
                }
            },
            remoteMediator = RoomsRemoteMediator(
                conversationRoomDao = conversationRoomDao,
                pagingMetaDao = pagingMetaDao,
                getItems = ::getSyncData,
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

    /** Updates a network connection */
    suspend fun patchNetworkConnection(publicId: String, proximity: Float): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            networkDao.updateProximity(
                ownerPublicId = Firebase.auth.currentUser?.uid,
                publicId = publicId,
                proximity = proximity
            )

            httpClient.safeRequest<NetworkListResponse> {
                patch(
                    urlString = "/api/v1/social/network/users/{$publicId}",
                    block = {
                        setBody(SocialConnectionUpdate(proximity = proximity))
                    }
                )
            }
        }
    }

    companion object {
        const val INITIAL_BATCH = "initial_batch"

        private val DEMO_SYNC = SyncResponse(
            nextBatch = null,
            rooms = RoomsResponseIO(
                join = hashMapOf(
                    "1" to ConversationRoomIO(
                        id = "1",
                        summary = RoomSummary(
                            heroes = listOf("1"),
                            lastMessage = MatrixEventContent.RoomMessageEvent(body = "Hey, what's up?"),
                            joinedMemberCount = 2
                        ),
                        proximity = 5f
                    ),
                    "2" to ConversationRoomIO(
                        id = "2",
                        unreadNotifications = RoomNotificationsCount(highlightCount = 2),
                        summary = RoomSummary(
                            heroes = listOf("2"),
                            canonicalAlias = "Gamer's room",
                            lastMessage = MatrixEventContent.RoomMessageEvent(body = "That's terrible:D"),
                            joinedMemberCount = 2
                        ),
                        proximity = 2f
                    )
                ),
                invite = hashMapOf(),
                knock = hashMapOf(),
                leave = hashMapOf()
            )
        )
    }
}