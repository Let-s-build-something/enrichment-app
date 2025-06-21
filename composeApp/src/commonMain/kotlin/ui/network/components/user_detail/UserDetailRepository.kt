package ui.network.components.user_detail

import data.io.user.NetworkItemIO
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomMemberDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class UserDetailRepository(
    private val roomMemberDao: RoomMemberDao,
    private val presenceEventDao: PresenceEventDao
) {

    suspend fun getUser(
        userId: String
    ): NetworkItemIO? = withContext(Dispatchers.IO) {
        roomMemberDao.get(userId = userId)?.toNetworkItem()?.copy(
            presence = presenceEventDao.get(userId)?.content
        )
    }
}