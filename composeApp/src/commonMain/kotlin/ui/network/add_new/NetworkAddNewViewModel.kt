package ui.network.add_new

import androidx.lifecycle.viewModelScope
import data.io.base.BaseResponse
import data.io.social.network.CircleRequestResponse
import data.io.social.network.CirclingRequest
import data.shared.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Communication between the UI, the control layers, and control and data layers */
class NetworkAddNewViewModel(
    private val repository: NetworkAddNewRepository
): SharedViewModel() {

    private val _response: MutableSharedFlow<BaseResponse<CircleRequestResponse>?> = MutableSharedFlow()
    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** response from last user's inclusion */
    val response = _response.asSharedFlow()

    /** whether request is currently in progress */
    val isLoading = _isLoading.asStateFlow()

    /** Makes a request for user's inclusion to one's social network */
    fun includeNewUser(displayName: String, tag: String) {
        viewModelScope.launch {
            _isLoading.emit(true)
            _response.emit(
                repository.includeNewUser(
                    CirclingRequest(
                        displayName = displayName,
                        tag = tag
                    )
                )
            )
            delay(200)
            _isLoading.emit(false)
        }
    }
}

internal val networkAddNewModule = module {
    factory { NetworkAddNewRepository(get()) }
    viewModelOf(::NetworkAddNewViewModel)
}