package ui.account

import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.set
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    /** Sets the theme of the app */
    fun updateTheme(choiceOrdinal: Int) {
        viewModelScope.launch {
            val choice = ThemeChoice.entries[choiceOrdinal]
            dataManager.localSettings.update {
                it?.copy(theme = choice)
            }
            settings[SettingsKeys.KEY_THEME] = choice.name
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