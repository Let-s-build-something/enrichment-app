package ui.network.profile

import androidx.lifecycle.viewModelScope
import data.io.base.BaseResponse
import data.io.social.network.request.CircleRequestResponse
import data.io.social.network.request.CirclingRequest
import data.io.user.NetworkItemIO
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val userProfileModule = module {
    factory { UserProfileRepository(get()) }
    viewModelOf(::UserProfileModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class UserProfileModel(
    private val repository: UserProfileRepository
): SharedModel() {

    private val _responseProfile = MutableStateFlow<BaseResponse<NetworkItemIO>>(BaseResponse.Idle)

    /** Result of the requested user profile */
    val responseProfile = _responseProfile.asStateFlow()

    private val _responseInclusion = MutableStateFlow<BaseResponse<CircleRequestResponse>>(BaseResponse.Idle)

    /** Result of the requested user profile */
    val responseInclusion = _responseInclusion.asStateFlow()

    /** Makes a request for a user profile */
    fun getUserProfile(publicId: String) {
        _responseInclusion.value = BaseResponse.Idle
        _responseProfile.value = BaseResponse.Loading
        viewModelScope.launch {
            _responseProfile.value = repository.getUserProfile(publicId)
        }
    }

    /** Makes a request for user's inclusion to one's social network */
    fun includeNewUser(displayName: String, tag: String) {
        _responseInclusion.value = BaseResponse.Loading
        viewModelScope.launch {
            delay(200)
            _responseInclusion.emit(
                repository.includeNewUser(
                    CirclingRequest(displayName = displayName, tag = tag)
                )
            )
        }
    }

    /** Makes a request for changes of proximity related to the [selectedConnections] */
    fun requestProximityChange(
        selectedConnections: List<String>,
        proximity: Float,
        onOperationDone: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedConnections.forEach { publicId ->
                // TODO change locally once the local database is in place
                repository.patchNetworkConnection(
                    publicId = publicId,
                    proximity = proximity
                )
            }
            onOperationDone()
        }
    }
}
