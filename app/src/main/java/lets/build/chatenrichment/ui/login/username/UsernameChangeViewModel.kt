package lets.build.chatenrichment.ui.login.username

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squadris.squadris.compose.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lets.build.chatenrichment.data.shared.SharedDataManager
import lets.build.chatenrichment.data.shared.io.user.UserProfile
import javax.inject.Inject

@HiltViewModel
class UsernameChangeViewModel @Inject constructor(
    private val repository: UsernameChangeRepository,
    private val sharedDataManager: SharedDataManager
): BaseViewModel() {

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** Logs out the currently signed in user */
    fun logoutCurrentUser() {
        Firebase.auth.signOut()
        sharedDataManager.currentUser.value = null
    }

    /** Logs out the currently signed in user */
    fun saveName(name: String) {
        sharedDataManager.userProfile.value = UserProfile("I am loading...")

        viewModelScope.launch {
            currentUser.value?.uid?.let { uid ->
                if(repository.saveUserProfile(
                        uid = uid,
                        data = UserProfile(name)
                    ).isSuccessful
                ) {
                    sharedDataManager.userProfile.value = UserProfile(name)
                }else {
                    sharedDataManager.userProfile.value = UserProfile()
                }
            }
        }
    }
}