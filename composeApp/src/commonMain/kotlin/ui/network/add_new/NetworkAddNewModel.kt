package ui.network.add_new

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import base.utils.tagToColor
import data.NetworkProximityCategory
import data.io.base.BaseResponse
import data.io.social.network.request.CircleRequestResponse
import data.io.social.network.request.CirclingRequest
import data.io.user.NetworkItemIO
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Communication between the UI, the control layers, and control and data layers */
class NetworkAddNewModel(
    private val repository: NetworkAddNewRepository
): SharedModel() {

    private val _response: MutableSharedFlow<BaseResponse<CircleRequestResponse>?> = MutableSharedFlow()
    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _recommendedUsers: MutableStateFlow<Map<NetworkProximityCategory, List<NetworkItemIO>>?> = MutableStateFlow(null)

    /** response from last user's inclusion */
    val response = _response.asSharedFlow()

    /** whether request is currently in progress */
    val isLoading = _isLoading.asStateFlow()

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

    /** Recommended users from all of social circle categories */
    val recommendedUsers = _recommendedUsers.asStateFlow()

    /** Makes a request for user recommendations from the local database */
    fun requestRecommendedUsers(excludeId: String?) {
        viewModelScope.launch {
            repository.getUserRecommendations(
                takeCount = TOP_ITEMS,
                excludeId = excludeId,
                ownerPublicId = currentUser.value?.matrixUserId
            ).success?.data.let { data ->
                _recommendedUsers.value = data
            }
        }
    }

    /** Makes a request for user's inclusion to one's social network */
    fun includeNewUser(
        displayName: String,
        tag: String,
        proximity: NetworkProximityCategory
    ) {
        viewModelScope.launch {
            _isLoading.emit(true)
            _response.emit(
                repository.includeNewUser(
                    CirclingRequest(
                        displayName = displayName,
                        tag = tag,
                        proximity = proximity.range.start
                    )
                )
            )
            delay(200)
            _isLoading.emit(false)
        }
    }

    companion object {
        private const val TOP_ITEMS = 4
    }
}

internal val networkAddNewModule = module {
    factory { NetworkAddNewRepository(get(), get(), get()) }
    viewModelOf(::NetworkAddNewModel)
}