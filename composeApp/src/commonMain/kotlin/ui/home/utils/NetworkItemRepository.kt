package ui.home.utils

import data.io.base.BaseResponse
import data.io.social.network.conversation.InvitationResponse
import data.io.social.network.conversation.RoomInvitationRequest
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.social.network.conversation.matrix.RoomSummary
import data.io.social.network.conversation.matrix.RoomType
import data.io.social.network.request.NetworkListResponse
import data.io.user.NetworkItemIO
import data.shared.DemoData
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.home.HomeRepository.Companion.INITIAL_BATCH
import ui.login.safeRequest
import ui.network.connection.SocialConnectionUpdate

class NetworkItemRepository(
    private val httpClient: HttpClient,
    private val networkItemDao: NetworkItemDao,
    private val conversationRoomDao: ConversationRoomDao
) {
    /** returns a list of network list */
    suspend fun getNetworkItems(): List<NetworkItemIO>? {
        return withContext(Dispatchers.IO) {
            networkItemDao.getNonFiltered()
        }
    }

    /** Updates a conversation's proximity */
    suspend fun patchProximity(
        conversationId: String?,
        publicId: String?,
        proximity: Float
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            if(publicId != null) {
                networkItemDao.updateProximity(
                    publicId = publicId,
                    proximity = proximity
                )
                conversationRoomDao.updateProximity(
                    id = conversationId,
                    proximity = proximity
                )
            }else {
                conversationRoomDao.updateProximity(
                    id = conversationId,
                    proximity = proximity
                )
            }

            httpClient.safeRequest<NetworkListResponse> {
                patch(
                    urlString = if(publicId != null) {
                        "/api/v1/social/user/$publicId"
                    }else "/api/v1/social/conversation/$conversationId",
                    block = {
                        setBody(SocialConnectionUpdate(
                            proximity = proximity
                        ))
                    }
                )
            }
        }
    }

    /** Creates a new invitation */
    suspend fun inviteToConversation(
        conversationId: String?,
        userPublicIds: List<String>?,
        message: String?,
        newName: String?
    ): BaseResponse<InvitationResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<InvitationResponse> {
                post(
                    urlString = "/api/v1/social/conversation/invite",
                    block = {
                        setBody(
                            RoomInvitationRequest(
                                conversationId = conversationId,
                                userPublicIds = userPublicIds,
                                message = message,
                                newRoomName = newName
                            )
                        )
                    }
                )
            }.let { response ->
                if(newName != null) {
                    (response.success?.data?.conversationId ?: DemoData.newRoomId).let { newId ->
                        conversationRoomDao.insertAll(
                            listOf(
                                ConversationRoomIO(
                                    id = newId,
                                    summary = RoomSummary(
                                        canonicalAlias = newName,
                                        isDirect = false,
                                        members = networkItemDao.getItems(
                                            userPublicIds = userPublicIds
                                        )
                                    )
                                ).apply {
                                    batch = INITIAL_BATCH
                                    ownerPublicId = Firebase.auth.currentUser?.uid
                                }
                            )
                        )
                        BaseResponse.Success(
                            InvitationResponse(
                                alias = newName,
                                conversationId = newId
                            )
                        )
                    }
                }else response
            }
        }
    }

    /** Retrieves all open rooms */
    suspend fun getOpenRooms(): List<ConversationRoomIO>? {
        return withContext(Dispatchers.IO) {
            conversationRoomDao.getNonFiltered()?.filter {
                it.type == RoomType.Joined && it.summary?.isDirect != true
            }
        }
    }
}