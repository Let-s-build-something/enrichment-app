package ui.network.components.user_detail

import data.io.user.NetworkItemIO
import database.dao.PresenceEventDao
import database.dao.RoomMemberDao
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

class UserDetailRepository(
    private val roomMemberDao: RoomMemberDao,
    private val presenceEventDao: PresenceEventDao,
    private val httpClient: HttpClient
) {

    suspend fun getUser(
        userId: String,
        homeserver: String
    ): NetworkItemIO? = withContext(Dispatchers.IO) {
        (roomMemberDao.get(userId = userId)?.toNetworkItem() ?: getRemoteUser(userId, homeserver))?.copy(
            presence = presenceEventDao.get(userId)?.content,
            userId = userId
        )
    }

    private suspend fun getRemoteUser(
        userId: String,
        homeserver: String
    ) = httpClient.safeRequest<NetworkItemIO> {
        httpClient.get(urlString = "https://${homeserver}/_matrix/client/v3/profile/${userId}")
    }.success?.data
}