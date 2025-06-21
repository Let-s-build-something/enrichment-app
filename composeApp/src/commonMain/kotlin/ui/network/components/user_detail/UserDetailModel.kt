package ui.network.components.user_detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import base.utils.tagToColor
import data.NetworkProximityCategory
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
    factory { UserDetailRepository(get(), get()) }
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
    private val _user = MutableStateFlow<NetworkItemIO?>(null)
    val user = _user.asStateFlow()

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
        if (userId != null) getUser(userId) else if (networkItem != null) {
            _user.value = networkItem
        }
    }

    private fun getUser(userId: String) {
        viewModelScope.launch {
            _user.value = repository.getUser(userId)
        }
    }
}