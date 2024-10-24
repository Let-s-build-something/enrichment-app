package ui.login.username

import androidx.lifecycle.viewModelScope
import data.io.base.BaseResponse
import data.io.social.username.ResponseUsernameChange
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UsernameChangeViewModel (
    private val repository: UsernameChangeRepository
): SharedViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _response = MutableStateFlow<BaseResponse<ResponseUsernameChange>?>(null)

    /** whether there is a pending request */
    val isLoading = _isLoading.asStateFlow()

    /** response to the latest request */
    val response = _response.asStateFlow()

    /** Makes a request to change user's username */
    fun requestUsernameChange(username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _response.value = repository.changeUsername(username).apply {
                success?.data?.let { data ->
                    sharedDataManager.currentUser.update { old ->
                        old?.copy(
                            username = data.username ?: old.username,
                            tag = data.tag ?: old.tag
                        )
                    }
                }
            }
            _isLoading.value = false
        }
    }
}