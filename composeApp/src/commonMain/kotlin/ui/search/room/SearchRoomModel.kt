package ui.search.room

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import data.io.DELAY_BETWEEN_REQUESTS_SHORT
import data.io.base.BaseResponse
import data.shared.SharedModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.homeserver_picker.HomeserverPickerModel.HomeserverAddress

internal val searchRoomModule = module {
    factory { SearchRoomRepository(get()) }
    viewModelOf(::SearchRoomModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class SearchRoomModel(
    private val repository: SearchRoomRepository
): SharedModel() {

    companion object {
        const val ITEMS_COUNT = 15
    }

    private val _state = MutableStateFlow<BaseResponse<Any>>(BaseResponse.Idle)
    private val _query = MutableStateFlow("")
    private val _selectedHomeserver = MutableStateFlow<HomeserverAddress?>(null)

    val selectedHomeserver = _selectedHomeserver.asStateFlow()
    val state = _state.asStateFlow()

    val rooms = repository.getRooms(
        query = { _query.value },
        queryHomeserver = { _selectedHomeserver.value?.address },
        config = PagingConfig(
            pageSize = ITEMS_COUNT,
            enablePlaceholders = true
        ),
        homeserver = { homeserverAddress }
    ).flow.cachedIn(viewModelScope)

    fun setIdleState() {
        _state.value = BaseResponse.Idle
    }

    fun selectHomeserver(homeserver: HomeserverAddress?) {
        viewModelScope.launch {
            _selectedHomeserver.value = homeserver
            repository.invalidateLocalSource()
            _state.value = BaseResponse.Loading
        }
    }

    private val queryScope = CoroutineScope(Job())
    fun queryRooms(prompt: CharSequence) {
        if (prompt == _query.value) return

        queryScope.coroutineContext.cancelChildren()
        queryScope.launch {
            _state.value = BaseResponse.Loading
            _query.value = prompt.toString()
            delay(DELAY_BETWEEN_REQUESTS_SHORT)
            repository.invalidateLocalSource()
        }
    }
}
