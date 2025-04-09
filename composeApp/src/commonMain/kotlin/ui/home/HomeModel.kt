package ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import augmy.interactive.shared.utils.PersistentListData
import base.utils.asSimpleString
import base.utils.tagToColor
import components.pull_refresh.RefreshableViewModel
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_REFRESH_DELAY
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_RESPONSE_DELAY
import data.NetworkProximityCategory
import data.io.app.SettingsKeys
import data.io.app.SettingsKeys.KEY_NETWORK_CATEGORIES
import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.toResponse
import data.io.matrix.room.ConversationRoomIO
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.home.utils.NetworkItemUseCase
import ui.home.utils.networkItemModule

internal val homeModule = module {
    includes(networkItemModule)
    factory { HomeRepository(get()) }
    factory { HomeModel(get<HomeRepository>(), get()) }
    viewModelOf(::HomeModel)
}


/** Communication between the UI, the control layers, and control and data layers */
class HomeModel(
    private val repository: HomeRepository,
    private val networkItemUseCase: NetworkItemUseCase
): SharedModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _categories = MutableStateFlow(NetworkProximityCategory.entries.toList())
    private val _requestResponse: MutableStateFlow<HashMap<String, BaseResponse<Any>?>> = MutableStateFlow(
        hashMapOf()
    )

    // firstVisibleItemIndex to firstVisibleItemScrollOffset
    var persistentPositionData: PersistentListData? = null

    /** Last selected network categories */
    val categories = _categories.transform { categories ->
        emit(
            categories.sortedBy {
                NetworkProximityCategory.entries.indexOf(it) + 1
            }
        )
    }

    /** Customized colors */
    val customColors: Flow<Map<NetworkProximityCategory, Color>> = localSettings.map { settings ->
        withContext(Dispatchers.Default) {
            settings?.networkColors?.mapIndexedNotNull { index, s ->
                tagToColor(s)?.let { color ->
                    NetworkProximityCategory.entries[index] to color
                }
            }.orEmpty().toMap()
        }
    }

    val requestResponse = _requestResponse.asStateFlow()
    val openConversations = networkItemUseCase.openConversations
    val isLoading = networkItemUseCase.isLoading
    val invitationResponse = networkItemUseCase.invitationResponse
    val networkItems = networkItemUseCase.networkItems.combine(_categories) { networkItems, categories ->
        withContext(Dispatchers.Default) {
            networkItems?.filter { item ->
                categories.any { it.range.contains(item.proximity ?: 1f) }
            }
        }
    }

    /** flow of current requests */
    val conversationRooms: Flow<PagingData<ConversationRoomIO>> = repository.getConversationRoomPager(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        ),
        ownerPublic = { matrixUserId }
    ).flow
        .cachedIn(viewModelScope)
        .combine(_categories) { pagingData, categories ->
            withContext(Dispatchers.Default) {
                pagingData.filter { data ->
                    categories.any { it.range.contains(data.proximity ?: 1f) }
                }
            }
        }


    init {
        viewModelScope.launch(Dispatchers.IO) {
            _categories.value = settings.getStringOrNull(KEY_NETWORK_CATEGORIES)
                ?.split(",")
                ?.mapNotNull {
                    NetworkProximityCategory.entries.firstOrNull { category -> category.name == it }
                }
                ?: NetworkProximityCategory.entries
        }
        viewModelScope.launch {
            networkItemUseCase.getNetworkItems(ownerPublicId = matrixUserId)
        }
    }

    /** Filters currently downloaded network items */
    fun filterNetworkItems(filter: List<NetworkProximityCategory>) {
        viewModelScope.launch(Dispatchers.Default) {
            _categories.value = filter
            settings.putString(
                KEY_NETWORK_CATEGORIES,
                filter.joinToString(",")
            )
        }
    }

    /** Updates color preference */
    fun updateColorPreference(
        category: NetworkProximityCategory,
        color: Color
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedDataManager.localSettings.update {
                it?.copy(
                    networkColors = it.networkColors.toMutableList().apply {
                        set(category.ordinal, color.asSimpleString())
                    }
                )
            }
            settings.putString(
                "${SettingsKeys.KEY_NETWORK_COLORS}_$matrixUserId",
                sharedDataManager.localSettings.value?.networkColors?.joinToString(",") ?: ""
            )
        }
    }

    /** Makes a request for all open rooms */
    fun requestOpenRooms() {
        viewModelScope.launch {
            networkItemUseCase.requestOpenRooms(matrixUserId)
        }
    }

    /** User response to an invitation */
    fun respondToInvitation(
        roomId: String?,
        accept: Boolean
    ) {
        if(roomId == null || _requestResponse.value[roomId] != null) return

        viewModelScope.launch {
            _requestResponse.update {
                hashMapOf(*it.toList().toTypedArray(), roomId to BaseResponse.Loading)
            }
            val startTime = Clock.System.now().toEpochMilliseconds()

            val result = repository.respondToInvitation(
                client = sharedDataManager.matrixClient.value,
                matrixUserId = matrixUserId,
                roomId = roomId,
                accept = accept
            )?.toResponse()

            delay(kotlin.math.max(
                Clock.System.now().toEpochMilliseconds().minus(startTime),
                MINIMUM_RESPONSE_DELAY
            ))
            _requestResponse.update {
                hashMapOf(*it.toList().toTypedArray()).apply {
                    set(
                        roomId,
                        result
                    )
                }
            }
            // return back the option to take action after a delay
            if(result is BaseResponse.Error) {
                delay(MINIMUM_REFRESH_DELAY)
                _requestResponse.update {
                    hashMapOf(*it.toList().toTypedArray()).apply {
                        remove(roomId)
                    }
                }
            }
        }
    }

    /** Makes a request for a change of proximity of a conversation */
    fun requestProximityChange(
        conversationId: String?,
        publicId: String?,
        proximity: Float,
        onOperationDone: () -> Unit = {}
    ) {
        if(conversationId == null) return
        viewModelScope.launch {
            networkItemUseCase.requestProximityChange(
                conversationId = conversationId,
                publicId = publicId,
                proximity = proximity,
                ownerPublicId = matrixUserId
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
                newName = newName,
                ownerPublicId = matrixUserId
            )
        }
    }
}
