package ui.account.profile

import androidx.lifecycle.viewModelScope
import base.utils.fromByteArrayToData
import data.io.base.BaseResponse
import data.io.social.username.ResponseDisplayNameChange
import data.shared.SharedModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                                displayName = data.displayName ?: old.displayName,
                                tag = data.tag ?: old.tag
                            )
                        }
                    }
                }
            )
            _isLoading.value = false
        }
    }

    /** Makes a request to change user's profile picture */
    fun requestPictureChange(pictureUrl: String) {
        viewModelScope.launch {
            suspendRequestPictureChange(pictureUrl)
        }
    }

    private suspend fun suspendRequestPictureChange(pictureUrl: String) {
        if(firebaseUser.firstOrNull()?.photoURL == pictureUrl) return

        _isLoading.value = true
        withContext(Dispatchers.IO) {
            try {
                firebaseUser.firstOrNull()?.updateProfile(
                    photoUrl = pictureUrl
                )
                if(firebaseUser.firstOrNull()?.photoURL == pictureUrl) _isPictureChangeSuccess.emit(true)
            }catch(e: Exception) {
                _isPictureChangeSuccess.emit(false)
            }
        }
        _isLoading.value = false
    }

    /** Makes a request to change user's profile picture */
    fun requestPictureUpload(
        mediaByteArray: ByteArray?,
        fileName: String
    ) {
        if(mediaByteArray == null) return

        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.Default) {
                val previousUrl = "${try { firebaseUser.firstOrNull()?.photoURL }catch (e: NotImplementedError) { null }}"

                val previousFileSuffix = """.+profile-picture(\.\w*).+""".toRegex()
                    .matchEntire(previousUrl)
                    ?.groupValues
                    ?.getOrNull(1)

                _isPictureChangeSuccess.emit(
                    uploadPictureStorage(
                        byteArray = mediaByteArray,
                        fileName = fileName,
                        previousFileSuffix = previousFileSuffix
                    )
                )
            }
            _isLoading.value = false
        }
    }

    /** @return if true, it was successful, if false, it failed */
    private suspend fun uploadPictureStorage(
        byteArray: ByteArray,
        fileName: String,
        previousFileSuffix: String?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val fileSuffix = ".${fileName.split(".").lastOrNull()}"

            val reference = Firebase.storage.reference.child(
                "user/${firebaseUser.firstOrNull()?.uid}/profile-picture$fileSuffix"
            )
            val previousReference = Firebase.storage.reference.child(
                "user/${firebaseUser.firstOrNull()?.uid}/profile-picture$previousFileSuffix"
            )

            reference.putData(fromByteArrayToData(byteArray))
            val newUrl = reference.getDownloadUrl()

            if(newUrl.isNotBlank()) {
                if(fileSuffix != previousFileSuffix) {
                    try {
                        previousReference.delete()
                    }catch (_: Exception) { }
                }
                requestPictureChange(newUrl)
                true
            }else false
        }
    }
}