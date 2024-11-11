package ui.network.profile

import androidx.lifecycle.viewModelScope
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val userProfileModule = module {
    factory { UserProfileRepository(get()) }
    viewModelOf(::UserProfileViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class UserProfileViewModel(
    private val repository: UserProfileRepository
): SharedViewModel() {

    private val _response = MutableStateFlow<BaseResponse<NetworkItemIO>>(BaseResponse.Loading)

    /** Result of the requested user profile */
    val response = _response.asStateFlow()

    /** Makes a request for a user profile */
    fun getUserProfile(publicId: String) {
        viewModelScope.launch {
            _response.value = repository.getUserProfile(publicId)
        }
    }
}