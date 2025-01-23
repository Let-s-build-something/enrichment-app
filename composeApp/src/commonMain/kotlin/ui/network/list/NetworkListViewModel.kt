package ui.network.list

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import base.utils.tagToColor
import components.pull_refresh.RefreshableViewModel
import data.NetworkProximityCategory
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

/** Communication between the UI, the control layers, and control and data layers */
class NetworkListViewModel(
    private val repository: NetworkListRepository
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _conversations = MutableStateFlow<List<ConversationRoomIO>?>(null)

    /** flow of current requests */
    val networkItems: Flow<PagingData<NetworkItemIO>> = repository.getNetworkListFlow(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        )
    ).flow.cachedIn(viewModelScope)

    /** List of all conversations on this device  */
    val conversations = _conversations.asStateFlow()

    /** Customized colors */
    val customColors: Flow<Map<NetworkProximityCategory, Color>> = localSettings.transform { settings ->
        settings?.networkColors?.mapIndexedNotNull { index, s ->
            tagToColor(s)?.let { color ->
                NetworkProximityCategory.entries[index] to color
            }
        }.orEmpty().toMap()
    }

    /** Makes a request for a change of proximity of a conversation */
    fun requestProximityChange(
        publicId: String?,
        proximity: Float,
        onOperationDone: () -> Unit = {}
    ) {
        if(publicId == null) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.patchNetworkProximity(
                publicId = publicId,
                proximity = proximity
            )
            onOperationDone()
        }
    }

    /** Makes a request to retrieve all the conversations */
    fun requestConversations() {
        viewModelScope.launch {
            _conversations.value = repository.getConversations()
        }
    }

    /** Creates a new invitation */
    fun inviteToConversation(
        conversationId: String?,
        userPublicId: String?,
        message: String?
    ) {
        if(conversationId == null || userPublicId == null) return
        viewModelScope.launch {
            repository.inviteToConversation(
                conversationId = conversationId,
                userPublicId = userPublicId,
                message = message
            )
        }
    }
}