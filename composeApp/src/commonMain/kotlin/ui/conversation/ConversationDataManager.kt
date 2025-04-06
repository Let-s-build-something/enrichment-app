package ui.conversation

import data.io.matrix.media.MediaRepositoryConfig
import data.io.matrix.room.ConversationRoomIO
import kotlinx.coroutines.flow.MutableStateFlow

class ConversationDataManager {
    val repositoryConfig = MutableStateFlow<MediaRepositoryConfig?>(null)
    val conversations = MutableStateFlow(hashMapOf<String, ConversationRoomIO>())
}