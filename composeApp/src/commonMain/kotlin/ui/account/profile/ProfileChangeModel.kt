package ui.account.profile

import androidx.lifecycle.viewModelScope
import data.io.base.BaseResponse
import data.io.social.username.ResponseDisplayNameChange
import data.shared.SharedModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import korlibs.io.net.MimeType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileChangeModel (
    private val repository: DisplayNameChangeRepository
): SharedModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _isPictureChangeSuccess = MutableSharedFlow<Boolean?>()
    private val _displayNameChangeResponse = MutableSharedFlow<BaseResponse<ResponseDisplayNameChange>?>()
    private val _displayNameValidationResponse = MutableStateFlow<BaseResponse<Any>?>(null)

    /** whether there is a pending request */
    val isLoading = _isLoading.asStateFlow()

    /** whether change of profile picture was successful */
    val isPictureChangeSuccess = _isPictureChangeSuccess.asSharedFlow()

    /** response to the latest request for display name change */
    val displayNameChangeResponse = _displayNameChangeResponse.asSharedFlow()

    /** response to the latest request display name input validation */
    val displayNameValidationResponse = _displayNameValidationResponse.asStateFlow()

    /** Validates currently entered display name */
    fun validateDisplayName(value: CharSequence) {
        if(value == sharedDataManager.currentUser.value?.displayName) return

        viewModelScope.launch {
            _isLoading.value = true
            _displayNameValidationResponse.value = repository.validateDisplayName(value)
            _isLoading.value = false
        }
    }

    /** Makes a request to change user's username */
    fun requestDisplayNameChange(value: CharSequence) {
        _isLoading.value = true
        viewModelScope.launch {
            _displayNameChangeResponse.emit(
                repository.changeDisplayName(value).apply {
                    success?.data?.let { data ->
                        sharedDataManager.currentUser.update { old ->
                            old?.copy(
                                displayName = data.displayName ?: old.displayName
                            )
                        }
                    }
                }
            )
            _isLoading.value = false
        }
    }

    /** Makes a request to change user's profile picture */
    fun requestPictureChange(
        fileUrl: String,
        localFile: PlatformFile?
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            if (localFile != null) {
                repository.uploadMedia(
                    mediaByteArray = localFile.readBytes(),
                    mimetype = MimeType.getByExtension(localFile.extension).mime,
                    homeserver = homeserver,
                    fileName = localFile.name
                ).also { response ->
                    val url = response.success?.data?.contentUri
                    if(url == null) {
                        _isPictureChangeSuccess.emit(false)
                        _isLoading.value = false
                    }else requestPictureChange(fileUrl = url, localFile = null)
                }
            } else {
                _isPictureChangeSuccess.emit(
                    if (matrixClient?.setAvatarUrl(fileUrl)?.isSuccess == true) {
                        sharedDataManager.currentUser.update {
                            it?.copy(avatarUrl = fileUrl)
                        }
                        true
                    }else false
                )
                _isLoading.value = false
            }
        }
    }
}