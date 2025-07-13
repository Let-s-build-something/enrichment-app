package ui.login.homeserver_picker

import androidx.lifecycle.ViewModel
import data.io.DELAY_BETWEEN_REQUESTS_SHORT
import data.io.base.BaseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val homeserverPickerModule = module {
    factory { HomeserverPickerRepository(get()) }
    viewModelOf(::HomeserverPickerModel)
}

class HomeserverPickerModel(
    private val repository: HomeserverPickerRepository
): ViewModel() {
    companion object {
        private const val HOMESERVER_REGEX = """^([a-zA-Z0-9.-]+|\[[0-9a-fA-F:.]+\])(:\d+)?$"""
    }

    private val _state = MutableStateFlow<BaseResponse<Any>>(BaseResponse.Idle)
    val state = _state.asStateFlow()

    private val validateScope = CoroutineScope(Job())
    fun validateHomeserver(homeserver: CharSequence) {
        if (_state.value !is BaseResponse.Idle) return
        if (!homeserver.matches(Regex(HOMESERVER_REGEX))) {
            _state.value = BaseResponse.Error()
            return
        }

        _state.value = BaseResponse.Loading
        validateScope.coroutineContext.cancelChildren()
        validateScope.launch {
            delay(DELAY_BETWEEN_REQUESTS_SHORT)
            repository.getWellKnown(homeserver.toString()).data?.server.let { server ->
                if (server != null) {
                    repository.validateHomeserver(server).also { response ->
                        _state.value = response
                    }
                } else _state.value = BaseResponse.Error() 
            }
        }
    }
}