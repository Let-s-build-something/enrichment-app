package ui.network.received

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import components.pull_refresh.RefreshableViewModel
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_REFRESH_DELAY
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_RESPONSE_DELAY
import data.io.base.BaseResponse
import data.io.social.network.request.CirclingActionRequest
import data.io.social.network.request.CirclingRequest
import data.shared.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.home.utils.networkItemModule
import ui.network.list.NetworkListRepository
import ui.network.list.NetworkListViewModel

/** Communication between the UI, the control layers, and control and data layers */
class NetworkReceivedViewModel(
    private val repository: NetworkReceivedRepository
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
    val requests: Flow<PagingData<CirclingRequest>> = repository.getRequestsFlow(
        PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        )
    ).flow.cachedIn(viewModelScope)

    /** Makes a request to accept the circle request */
    fun acceptRequest(uid: String, accept: Boolean) {
        if(_response.value[uid] != null) return

        viewModelScope.launch {
            _response.update {
                hashMapOf(*it.toList().toTypedArray(), uid to BaseResponse.Loading)
            }
            val startTime = Clock.System.now().toEpochMilliseconds()

            val response = repository.acceptRequest(
                CirclingActionRequest(uid = uid, accept = accept)
            )

            delay(kotlin.math.max(
                Clock.System.now().toEpochMilliseconds().minus(startTime),
                MINIMUM_RESPONSE_DELAY
            ))
            _response.update {
                hashMapOf(*it.toList().toTypedArray()).apply {
                    set(
                        uid,
                        response
                    )
                }
            }
            // return back the option to take action after a delay
            if(response is BaseResponse.Error) {
                delay(MINIMUM_REFRESH_DELAY)
                _response.update {
                    hashMapOf(*it.toList().toTypedArray()).apply {
                        remove(uid)
                    }
                }
            }
        }
    }
}

internal val networkManagementModule = module {
    factory { NetworkReceivedRepository(get()) }
    viewModelOf(::NetworkReceivedViewModel)

    includes(networkItemModule)
    factory { NetworkListRepository(get(), get(), get()) }
    viewModelOf(::NetworkListViewModel)
}