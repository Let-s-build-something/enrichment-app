package lets.build.chatenrichment.data.shared

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.toObject
import com.squadris.squadris.compose.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lets.build.chatenrichment.data.shared.io.user.UserProfile
import javax.inject.Inject

@HiltViewModel
open class SharedViewModel @Inject constructor(
    private val sharedRepository: SharedRepository,
    private val sharedDataManager: SharedDataManager
): BaseViewModel() {

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** All user's information */
    val userProfile = sharedDataManager.userProfile.asStateFlow()


    /** Makes a request for a user profile */
    fun requestUserProfile() {
        currentUser.value?.uid?.let { uid ->
            viewModelScope.launch {
                sharedRepository.getUserProfile(uid).addOnCompleteListener {
                    sharedDataManager.userProfile.value = if(it.isSuccessful) {
                        it.result?.toObject<UserProfile>()
                    }else UserProfile()
                }
            }
        }
    }
}