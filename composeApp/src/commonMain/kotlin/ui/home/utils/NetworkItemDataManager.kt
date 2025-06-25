package ui.home.utils

import data.io.matrix.room.FullConversationRoom
import data.io.user.NetworkItemIO
import kotlinx.coroutines.flow.MutableStateFlow

class NetworkItemDataManager {
    /** List of open conversations */
    val openConversations = MutableStateFlow<List<FullConversationRoom>?>(null)

    /** Lit of network items */
    val networkItems = MutableStateFlow<List<NetworkItemIO>?>(null)
}