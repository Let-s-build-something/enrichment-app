package ui.account

import androidx.lifecycle.viewModelScope
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val accountDashboardModule = module {
    viewModelOf(::AccountDashboardViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class AccountDashboardViewModel: SharedViewModel() {

    /** Response to a sign out request */
    private val _signOutResponse = MutableStateFlow(false)

    /** Response to a sign out request */
    val signOutResponse = _signOutResponse.asStateFlow()

    /** fcm token of this device */
    private val _currentFcmToken = MutableStateFlow<String?>(null)

    /** fcm token of this device */
    val currentFcmToken = _currentFcmToken.asStateFlow()

    init {
        requestFcmToken()
    }

    /** makes a request for current fcm token of this device */
    private fun requestFcmToken() {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.fcmToken = dataManager.fcmToken ?: Firebase.messaging.getToken()
            _currentFcmToken.value = dataManager.fcmToken
        }
    }

    /** Logs out the currently signed in user */
    override fun logoutCurrentUser() {
        viewModelScope.launch {
            super.logoutCurrentUser()
            _signOutResponse.emit(Firebase.auth.currentUser == null)
        }
    }
}