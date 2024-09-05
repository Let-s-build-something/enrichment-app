package ui.account

import androidx.lifecycle.viewModelScope
import base.PlatformFirebase
import data.shared.SharedDataManager
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import ui.home.HomeViewModel.Companion.SETTINGS_KEY_AUTO_EMAIL
import ui.home.HomeViewModel.Companion.SETTINGS_KEY_AUTO_PASSWORD

internal val accountDashboardModule = module {
    viewModelOf(::AccountDashboardViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class AccountDashboardViewModel: SharedViewModel() {

    /** Response to a sign out request */
    private val _signOutResponse = MutableStateFlow(false)

    /** Response to a sign out request */
    val signOutResponse = _signOutResponse.asStateFlow()

    /** Logs out the currently signed in user */
    override fun logoutCurrentUser() {
        viewModelScope.launch {
            super.logoutCurrentUser()
            _signOutResponse.emit(PlatformFirebase?.auth?.currentUser == null)
        }
    }
}