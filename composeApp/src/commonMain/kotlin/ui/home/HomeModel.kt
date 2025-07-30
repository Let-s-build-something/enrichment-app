package ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
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
import data.io.matrix.room.FullConversationRoom
import data.io.social.network.conversation.message.FullConversationMessage
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.folivo.trixnity.clientserverapi.client.SyncState
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.home.utils.NetworkItemUseCase
import ui.home.utils.networkItemModule
import utils.SharedLogger

internal val homeModule = module {
    includes(networkItemModule)
    factory { HomeRepository(get(), get()) }
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
    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {
        syncService.restart()
    }

    enum class UiMode {
        List,
        Circle,
        Loading,
        NoClient;

        val isFinished: Boolean
            get() = this == List || this == Circle
    }

    private val _uiMode = MutableStateFlow<UiMode>(
        if (authService.awaitingAutologin || matrixClient != null) UiMode.List else UiMode.NoClient
    )
    private val _selectedUserId = MutableStateFlow<String?>(null)
    private val _categories = MutableStateFlow(NetworkProximityCategory.entries.toList())
    private val _searchQuery = MutableStateFlow("")
    private val _collapsedRooms = MutableStateFlow(listOf<String>())
    private val _requestResponse: MutableStateFlow<HashMap<String, BaseResponse<Any>?>> = MutableStateFlow(
        hashMapOf()
    )

    /** firstVisibleItemIndex to firstVisibleItemScrollOffset */
    var persistentPositionData: PersistentListData? = null

    val collapsedRooms = _collapsedRooms.asStateFlow()
    val uiMode = _uiMode.asStateFlow()
    val selectedUserId = _selectedUserId.asStateFlow()

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
    val isLoading = networkItemUseCase.isLoading

    private val roomsFlow = repository.getConversationRoomPager(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        ),
        ownerPublic = { matrixUserId }
    ).flow.cachedIn(viewModelScope)

    /** flow of current requests */
    val conversationRooms: Flow<PagingData<FullConversationRoom>> = combine(
        roomsFlow,
        _collapsedRooms,
        _searchQuery,
        _categories
    ) { rooms, collapsedRooms, query, categories ->
        withContext(Dispatchers.Default) {
            rooms.map { room ->
                room.apply {
                    messages = if (!collapsedRooms.contains(room.id)) {
                        queryMessagesOfRoom(query, room)
                    } else listOf()
                }
            }.filter { data ->
                (query.isBlank() || data.messages.isNotEmpty())
                        && categories.any { it.range.contains(data.data.proximity ?: 1f) }
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

        viewModelScope.launch {
            sharedDataManager.matrixClient.shareIn(this, started = SharingStarted.Eagerly).collectLatest { client ->
                if (client == null) {
                    _uiMode.value = UiMode.NoClient
                } else {
                    viewModelScope.launch {
                        client.syncState.collect { syncState ->
                            SharedLogger.logger.debug { "HomeModel, syncState: $syncState" }
                            when {
                                syncState == SyncState.INITIAL_SYNC -> {
                                    _uiMode.value = UiMode.Loading
                                }
                                syncState == SyncState.RUNNING && !_uiMode.value.isFinished -> {
                                    _uiMode.value = UiMode.List
                                }
                                syncState == SyncState.STOPPED -> onDataRequest(isSpecial = true)
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectUser(room: FullConversationRoom?) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                if (room == null || room.data.summary?.isDirect == false) {
                    _selectedUserId.value = null
                    return@withContext
                }

                _selectedUserId.value = room.data.summary?.heroes?.firstOrNull()?.full
                    ?: room.members.firstOrNull()?.userId
            }
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

    fun searchForMessages(query: CharSequence) {
        _searchQuery.value = query.toString()
    }

    fun collapseRoom(roomId: String) {
        _collapsedRooms.update {
            if (it.contains(roomId)) it.minus(roomId) else it.plus(roomId)
        }
    }

    private suspend fun queryMessagesOfRoom(
        query: String,
        room: FullConversationRoom
    ): List<FullConversationMessage> {
        return withContext(Dispatchers.Default) {
            val limit = 10

            repository.queryLocalMessagesOfRoom(
                roomId = room.id,
                query = query,
                limit = limit
            ).let { localMessages ->
                if (localMessages.size < limit) {
                    /* TODO repository.queryAndInsertMessages(
                        matrixClient = matrixClient,
                        query = query,
                        roomId = room.id,
                        limit = limit - localMessages.size
                    )*/

                    repository.queryLocalMessagesOfRoom(
                        roomId = room.id,
                        query = query,
                        limit = limit
                    )
                }else localMessages
            }
        }
    }

    fun swapUiMode(isList: Boolean) {
        _uiMode.value = if(isList) UiMode.List else UiMode.Circle
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
