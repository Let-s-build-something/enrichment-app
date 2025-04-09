package ui.conversation

import data.io.matrix.media.MediaRepositoryConfig
import data.io.matrix.room.ConversationRoomIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ConversationDataManager {
    val repositoryConfig = MutableStateFlow<MediaRepositoryConfig?>(null)
    val conversations = MutableStateFlow("" to hashMapOf<String, ConversationRoomIO>())

    @OptIn(ExperimentalUuidApi::class)
    suspend fun updateConversations(
        function: suspend (HashMap<String, ConversationRoomIO>) -> HashMap<String, ConversationRoomIO>
    ) = withContext(Dispatchers.Default) {
        conversations.update {
            Uuid.random().toString() to function(it.second)
        }
    }
}