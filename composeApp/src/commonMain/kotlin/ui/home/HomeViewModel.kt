package ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import base.utils.asSimpleString
import base.utils.tagToColor
import com.russhwolf.settings.set
import components.pull_refresh.RefreshableViewModel
import data.NetworkProximityCategory
import data.io.app.SettingsKeys.KEY_NETWORK_CATEGORIES
import data.io.app.SettingsKeys.KEY_NETWORK_COLORS
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.network.list.NetworkListRepository
import ui.network.received.networkManagementModule

internal val homeModule = module {
    includes(networkManagementModule)
    factory { HomeViewModel(get<NetworkListRepository>()) }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel(
    private val repository: NetworkListRepository
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _categories = MutableStateFlow(
        settings.getStringOrNull(KEY_NETWORK_CATEGORIES)
            ?.split(",")
            ?.mapNotNull {
                NetworkProximityCategory.entries.firstOrNull { category -> category.name == it }
            }
            ?: listOf(
                NetworkProximityCategory.Family,
                NetworkProximityCategory.Peers
            )
    )

    /** Last selected network categories */
    val categories = _categories.transform {
        emit(
            it.sortedBy {
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

    /** flow of current requests */
    val networkItems: Flow<PagingData<NetworkItemIO>> = repository.getNetworkListFlow(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
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

    /** Filters currently downloaded network items */
    fun filterNetworkItems(filter: List<NetworkProximityCategory>) {
        viewModelScope.launch(Dispatchers.Default) {
            _categories.value = filter
            settings[KEY_NETWORK_CATEGORIES] = filter.joinToString(",")
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
                // TODO change locally once the local database is in place
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
        viewModelScope.launch(Dispatchers.Default) {
            sharedDataManager.localSettings.update {
                it?.copy(
                    networkColors = it.networkColors.toMutableList().apply {
                        set(category.ordinal, color.asSimpleString())
                    }
                )
            }
            settings[KEY_NETWORK_COLORS] = sharedDataManager.localSettings.value?.networkColors?.joinToString(",")
        }
    }
}
