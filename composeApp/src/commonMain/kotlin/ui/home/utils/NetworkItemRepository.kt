package ui.home.utils

import data.io.base.BaseResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomSummary
import data.io.matrix.room.RoomType
import data.io.social.network.conversation.InvitationResponse
import data.io.social.network.conversation.RoomInvitationRequest
import data.io.user.NetworkItemIO
import database.dao.ConversationRoomDao
import database.dao.NetworkItemDao
import database.dao.RoomMemberDao
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest
import ui.network.connection.SocialConnectionUpdate

class NetworkItemRepository(
    private val httpClient: HttpClient,
    private val networkItemDao: NetworkItemDao,
    private val roomMemberDao: RoomMemberDao,
    private val conversationRoomDao: ConversationRoomDao
) {
    /** returns a list of network list */
    suspend fun getNetworkItems(ownerPublicId: String?): List<NetworkItemIO> {
        return withContext(Dispatchers.IO) {
            networkItemDao.getNonFiltered(ownerPublicId)
        }
    }

    /** Updates a conversation's proximity */
    suspend fun patchProximity(
        conversationId: String?,
        ownerPublicId: String?,
        publicId: String?,
        proximity: Float
    ): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            if(publicId != null) {
                networkItemDao.updateProximity(
                    publicId = publicId,
                    proximity = proximity,
                    ownerPublicId = ownerPublicId
                )
                conversationRoomDao.updateProximity(
                    id = conversationId,
                    proximity = proximity,
                    ownerPublicId = ownerPublicId
                )
            }else {
                conversationRoomDao.updateProximity(
                    id = conversationId,
                    proximity = proximity,
                    ownerPublicId = ownerPublicId
                )
            }

            httpClient.safeRequest<Any> {
                patch(
                    urlString = if(publicId != null) {
                        "/api/v1/social/network/users/$publicId"
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
        ownerPublicId: String?,
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
                    response.success?.data?.conversationId?.let { newId ->
                        conversationRoomDao.insertAll(
                            listOf(
                                ConversationRoomIO(
                                    id = newId,
                                    summary = RoomSummary(
                                        canonicalAlias = newName,
                                        isDirect = false
                                    ).apply {
                                        members = roomMemberDao.get(userIds = userPublicIds.orEmpty())
                                    },
                                    ownerPublicId = ownerPublicId,
                                    type = RoomType.Joined
                                )
                            )
                        )
                        BaseResponse.Success(
                            InvitationResponse(
                                alias = newName,
                                conversationId = newId
                            )
                        )
                    } ?: BaseResponse.Error()
                }else response
            }
        }
    }

    /** Retrieves all open rooms */
    suspend fun getOpenRooms(ownerPublicId: String?): List<ConversationRoomIO> {
        return withContext(Dispatchers.IO) {
            conversationRoomDao.getNonFiltered(ownerPublicId).filter {
                it.type == RoomType.Joined && it.summary?.isDirect != true
            }
        }
    }
}