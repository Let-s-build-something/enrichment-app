package ui.account.profile

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import data.io.base.BaseResponse
import data.io.social.username.ResponseUsernameChange
import data.shared.SharedViewModel
import data.shared.fromByteArrayToData
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileChangeViewModel (
    private val repository: UsernameChangeRepository
): SharedViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _isPictureChangeSuccess = MutableSharedFlow<Boolean?>()
    private val _userNameResponse = MutableStateFlow<BaseResponse<ResponseUsernameChange>?>(null)

    /** whether there is a pending request */
    val isLoading = _isLoading.asStateFlow()

    /** whether change of profile picture was successful */
    val isPictureChangeSuccess = _isPictureChangeSuccess.asSharedFlow()

    /** response to the latest request for username change */
    val usernameResponse = _userNameResponse.asStateFlow()

    /** Makes a request to change user's username */
    fun requestUsernameChange(username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _userNameResponse.value = repository.changeUsername(username).apply {
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

    /** Makes a request to change user's profile picture */
    fun requestPictureChange(pictureUrl: String) {
        viewModelScope.launch {
            suspendRequestPictureChange(pictureUrl)
        }
    }

    private suspend fun suspendRequestPictureChange(pictureUrl: String) {
        if(firebaseUser.value?.photoURL == pictureUrl) return

        _isLoading.value = true
        withContext(Dispatchers.IO) {
            try {
                firebaseUser.value?.updateProfile(
                    photoUrl = pictureUrl
                )
                if(firebaseUser.value?.photoURL == pictureUrl) _isPictureChangeSuccess.emit(true)
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
                val previousUrl = "${try { firebaseUser.value?.photoURL }catch (e: NotImplementedError) { null }}"

                val previousFileSuffix = """.+profile-picture(\.\w*).+""".toRegex()
                    .matchEntire(previousUrl)
                    ?.groupValues
                    ?.getOrNull(1)

                if(currentPlatform != PlatformType.Jvm) {
                    _isPictureChangeSuccess.emit(
                        uploadPictureStorage(
                            byteArray = mediaByteArray,
                            fileName = fileName,
                            previousFileSuffix = previousFileSuffix
                        )
                    )
                }else {
                    uploadPictureApi(
                        byteArray = mediaByteArray,
                        fileName = fileName,
                        previousFileSuffix = previousFileSuffix
                    )
                }
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
                "${firebaseUser.value?.uid}/profile-picture$fileSuffix"
            )
            val previousReference = Firebase.storage.reference.child(
                "${firebaseUser.value?.uid}/profile-picture$previousFileSuffix"
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

    /** @return if true, it was successful, if false, it failed */
    private suspend fun uploadPictureApi(
        byteArray: ByteArray,
        fileName: String,
        previousFileSuffix: String?
    ): Boolean {
        currentUser.value?.idToken?.let { idToken ->
            repository.changeProfilePictureHttp(
                fileName = "${firebaseUser.value?.uid}%2Fprofile-picture.${fileName.split(".").lastOrNull()}",
                previousFileName = "${firebaseUser.value?.uid}%2Fprofile-picture$previousFileSuffix",
                byteArray = byteArray,
                idToken = idToken
            )?.let { newUrl ->
                requestPictureChange(newUrl)
                return true
            }
        }

        return false
    }
}