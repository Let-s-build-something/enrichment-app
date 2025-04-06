package ui.conversation.settings

import data.io.matrix.room.event.ConversationRoomMember
import database.dao.matrix.RoomMemberDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for calling APIs and remote work in general */
class ConversationSettingsRepository(
    private val roomMemberDao: RoomMemberDao
) {
    suspend fun getMembers(
        conversationId: String
    ): List<ConversationRoomMember> = withContext(Dispatchers.IO) {
        roomMemberDao.getOfRoom(roomId = conversationId)
    }
}
