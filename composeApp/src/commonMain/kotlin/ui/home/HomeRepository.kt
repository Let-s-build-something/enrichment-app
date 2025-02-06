package ui.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomType
import data.io.matrix.room.RoomsResponseIO
import data.io.matrix.SyncResponse
import database.dao.ConversationRoomDao
import database.dao.MatrixPagingMetaDao
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.login.safeRequest

class HomeRepository(
    private val httpClient: HttpClient,
    private val conversationRoomDao: ConversationRoomDao,
    private val pagingMetaDao: MatrixPagingMetaDao
) {
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
            }
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

    companion object {
        const val INITIAL_BATCH = "initial_batch"
    }
}