package ui.network.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import components.pull_refresh.RefreshableViewModel
import data.io.base.BaseResponse
import data.io.social.network.CirclingRequest
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val requests: Flow<PagingData<CirclingRequest>> = repository.getNetworkListFlow(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        )
    ).flow.cachedIn(viewModelScope)
}