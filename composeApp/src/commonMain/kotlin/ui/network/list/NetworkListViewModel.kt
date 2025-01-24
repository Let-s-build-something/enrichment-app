package ui.network.list

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import base.utils.tagToColor
import components.pull_refresh.RefreshableViewModel
import data.NetworkProximityCategory
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import ui.home.utils.NetworkItemUseCase

/** Communication between the UI, the control layers, and control and data layers */
class NetworkListViewModel(
    repository: NetworkListRepository,
    private val networkItemUseCase: NetworkItemUseCase
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    /** flow of current requests */
    val networkItems: Flow<PagingData<NetworkItemIO>> = repository.getNetworkListFlow(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        )
    ).flow.cachedIn(viewModelScope)

    /** Customized colors */
    val customColors: Flow<Map<NetworkProximityCategory, Color>> = localSettings.transform { settings ->
        settings?.networkColors?.mapIndexedNotNull { index, s ->
            tagToColor(s)?.let { color ->
                NetworkProximityCategory.entries[index] to color
            }
        }.orEmpty().toMap()
    }

    val openConversations = networkItemUseCase.openConversations
    val isLoading = networkItemUseCase.isLoading
    val response = networkItemUseCase.invitationResponse

    /** Makes a request for all open rooms */
    fun requestOpenConversations() {
        viewModelScope.launch {
            networkItemUseCase.requestOpenRooms()
        }
    }

    /** Makes a request for a change of proximity of a conversation */
    fun requestProximityChange(
        conversationId: String? = null,
        publicId: String?,
        proximity: Float,
        onOperationDone: () -> Unit = {}
    ) {
        if(conversationId == null) return
        viewModelScope.launch {
            networkItemUseCase.requestProximityChange(
                conversationId = conversationId,
                publicId = publicId,
                proximity = proximity
            )
            onOperationDone()
        }
    }

    /** Creates a new invitation to a conversation room */
    fun inviteToConversation(
        conversationId: String?,
        userPublicIds: List<String>?,
        message: String?,
        newName: String? = null
    ) {
        viewModelScope.launch {
            networkItemUseCase.inviteToConversation(
                conversationId = conversationId,
                userPublicIds = userPublicIds,
                message = message,
                newName = newName
            )
        }
    }
}