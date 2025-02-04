package ui.conversation.message

import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.user.NetworkItemIO
import database.dao.ConversationMessageDao
import database.dao.NetworkItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext


class MessageDetailRepository(
    private val conversationMessageDao: ConversationMessageDao,
    private val networkItemDao: NetworkItemDao
) {

    /** Retrieves singular message from the local DB */
    suspend fun getMessage(id: String): ConversationMessageIO? {
        return withContext(Dispatchers.IO) {
            conversationMessageDao.get(id)?.also {
                it.user = getUser(it.authorPublicId)
            }
        }
    }

    /** Retrieves singular message from the local DB */
    private suspend fun getUser(id: String?): NetworkItemIO? {
        return if(id == null) null else withContext(Dispatchers.IO) {
            networkItemDao.get(id)
        }
    }
}