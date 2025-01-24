package ui.home.utils

import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.user.NetworkItemIO
import kotlinx.coroutines.flow.MutableStateFlow

class NetworkItemDataManager {
    /** List of open conversations */
    val openConversations = MutableStateFlow<List<ConversationRoomIO>?>(null)

    /** Lit of network items */
    val networkItems = MutableStateFlow<List<NetworkItemIO>?>(null)
}