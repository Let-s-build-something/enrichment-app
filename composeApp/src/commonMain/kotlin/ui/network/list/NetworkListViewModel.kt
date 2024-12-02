package ui.network.list

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import base.tagToColor
import components.OptionsLayoutAction
import components.pull_refresh.RefreshableViewModel
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_REFRESH_DELAY
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_RESPONSE_DELAY
import data.NetworkProximityCategory
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** Communication between the UI, the control layers, and control and data layers */
class NetworkListViewModel(
    private val repository: NetworkListRepository
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    private val _response: MutableStateFlow<HashMap<String, BaseResponse<Any>?>> = MutableStateFlow(
        hashMapOf()
    )

    /** last response of user's action to a request */
    val response = _response.asStateFlow()

    /** flow of current requests */
    val requests: Flow<PagingData<NetworkItemIO>> = repository.getNetworkListFlow(
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

    /** Makes a request for an action */
    fun onNetworkAction(data: NetworkItemIO?, action: OptionsLayoutAction) {
        if(data?.userPublicId == null || _response.value[data.userPublicId] != null) return

        viewModelScope.launch {
            _response.update {
                hashMapOf(*it.toList().toTypedArray(), data.userPublicId to BaseResponse.Loading)
            }
            val startTime = Clock.System.now().toEpochMilliseconds()

            // TODO request repository

            delay(kotlin.math.max(
                Clock.System.now().toEpochMilliseconds().minus(startTime),
                MINIMUM_RESPONSE_DELAY
            ))
            /*_response.update {
                hashMapOf(*it.toList().toTypedArray()).apply {
                    set(
                        data.publicId,
                        response
                    )
                }
            }*/
            // return back the option to take action after a delay
            delay(MINIMUM_REFRESH_DELAY)
            _response.update {
                hashMapOf(*it.toList().toTypedArray()).apply {
                    remove(data.userPublicId)
                }
            }
        }
    }
}