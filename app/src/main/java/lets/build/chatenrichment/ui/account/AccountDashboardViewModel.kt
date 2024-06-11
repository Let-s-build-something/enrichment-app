package lets.build.chatenrichment.ui.account

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import com.squadris.squadris.compose.base.BaseViewModel
import lets.build.chatenrichment.data.shared.SharedDataManager
import javax.inject.Inject

@HiltViewModel
class AccountDashboardViewModel @Inject constructor(
    private val sharedDataManager: SharedDataManager
): BaseViewModel() {

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** Logs out the currently signed in user */
    fun logoutCurrentUser() {
        Firebase.auth.signOut()
        sharedDataManager.currentUser.value = null
    }
}