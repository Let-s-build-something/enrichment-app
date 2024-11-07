package ui.network.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import components.network.NetworkAction
import components.pull_refresh.RefreshableViewModel
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_REFRESH_DELAY
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_RESPONSE_DELAY
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** Makes a request for an action */
    fun onNetworkAction(data: NetworkItemIO?, action: NetworkAction) {
        if(data?.publicId == null || _response.value[data.publicId] != null) return

        viewModelScope.launch {
            _response.update {
                hashMapOf(*it.toList().toTypedArray(), data.publicId to BaseResponse.Loading)
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
                    remove(data.publicId)
                }
            }
        }
    }
}