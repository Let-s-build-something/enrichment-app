@file:OptIn(ExperimentalSettingsApi::class)

package ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import base.utils.asSimpleString
import base.utils.tagToColor
import com.russhwolf.settings.ExperimentalSettingsApi
import components.pull_refresh.RefreshableViewModel
import data.NetworkProximityCategory
import data.io.app.SettingsKeys.KEY_NETWORK_CATEGORIES
import data.io.app.SettingsKeys.KEY_NETWORK_COLORS
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.shared.SharedViewModel
import database.dao.NetworkItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    single { HomeDataManager() }
    factory { HomeRepository(get(), get(), get(), get()) }
    factory { HomeViewModel(get<HomeDataManager>(), get<HomeRepository>(), get()) }
    viewModelOf(::HomeViewModel)
}


/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel(
    private val dataManager: HomeDataManager,
    private val repository: HomeRepository,
    private val networkItemDao: NetworkItemDao
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {
        getNetworkItems()
    }

    private val _categories = MutableStateFlow(listOf<NetworkProximityCategory>())

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

    /** List of network items */
    val networkItems = dataManager.networkItems.asStateFlow()

    /** flow of current requests */
    val conversationRooms: Flow<PagingData<ConversationRoomIO>> = repository.getConversationRoomPager(
        PagingConfig(
            pageSize = 40,
            enablePlaceholders = true,
            initialLoadSize = 40
        )
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
            getNetworkItems()
        }
    }

    private suspend fun getNetworkItems() {
        if(dataManager.networkItems.value == null) {
            repository.getNetworkItems().success?.data?.content?.let {
                withContext(Dispatchers.Default) {
                    dataManager.networkItems.value = it.filter {
                        _categories.value.any { category -> category.range.contains(it.proximity ?: 1f) }
                    }
                    networkItemDao.insertAll(it)
                }
            }
        }
    }

    /** Filters currently downloaded network items */
    @OptIn(ExperimentalSettingsApi::class)
    fun filterNetworkItems(filter: List<NetworkProximityCategory>) {
        viewModelScope.launch(Dispatchers.Default) {
            _categories.value = filter
            settings.putString(
                KEY_NETWORK_CATEGORIES,
                filter.joinToString(",")
            )
        }
    }

    /** Makes a request for changes of proximity related to the [selectedConnections] */
    fun requestProximityChange(
        selectedConnections: List<String>,
        proximity: Float,
        onOperationDone: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedConnections.forEach { publicId ->
                repository.patchNetworkConnection(
                    publicId = publicId,
                    proximity = proximity
                )
            }
            onOperationDone()
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
                KEY_NETWORK_COLORS,
                sharedDataManager.localSettings.value?.networkColors?.joinToString(",") ?: ""
            )
        }
    }
}
