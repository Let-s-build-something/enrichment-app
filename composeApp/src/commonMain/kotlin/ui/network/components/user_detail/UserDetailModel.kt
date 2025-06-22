package ui.network.components.user_detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import base.utils.tagToColor
import data.NetworkProximityCategory
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val userDetailModule = module {
    factory { UserDetailRepository(get(), get(), get()) }
    factory { (userId: String?, itemIO: NetworkItemIO?) ->
        UserDetailModel(userId, itemIO, get())
    }
    viewModel { (userId: String?, itemIO: NetworkItemIO?) ->
        UserDetailModel(userId, itemIO, get())
    }
}

class UserDetailModel(
    userId: String?,
    networkItem: NetworkItemIO?,
    private val repository: UserDetailRepository
): SharedModel() {
    private val _response = MutableStateFlow<BaseResponse<NetworkItemIO>>(BaseResponse.Idle)
    val response = _response.asStateFlow()

    /** Customized social circle colors */
    val socialCircleColors: Flow<Map<NetworkProximityCategory, Color>> = localSettings.map { settings ->
        withContext(Dispatchers.Default) {
            settings?.networkColors?.mapIndexedNotNull { index, s ->
                tagToColor(s)?.let { color ->
                    NetworkProximityCategory.entries[index] to color
                }
            }.orEmpty().toMap()
        }
    }

    init {
        if (networkItem != null) _response.value = BaseResponse.Success(networkItem)
        (userId ?: networkItem?.userId)?.let { getUser(it) }
    }

    private fun getUser(userId: String) {
        _response.value = BaseResponse.Loading
        viewModelScope.launch {
            repository.getUser(userId, homeserver).let {
                _response.value = if (it != null) BaseResponse.Success(it) else BaseResponse.Error()
            }
        }
    }
}