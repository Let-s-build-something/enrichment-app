package ui.home

import androidx.lifecycle.viewModelScope
import base.PlatformFirebase
import data.io.CloudUserHelper
import data.shared.SharedViewModel
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    factory { HomeViewModel() }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel: SharedViewModel() {

    companion object {
        /** Settings key for autologin email */
        const val SETTINGS_KEY_AUTO_EMAIL = "SETTINGS_KEY_AUTO_EMAIL"

        /** Settings key for autologin password */
        const val SETTINGS_KEY_AUTO_PASSWORD = "SETTINGS_KEY_AUTO_PASSWORD"
    }


    init {
        checkForUser()
    }

    /**
     * Checks whether any user is signed in.
     * If no user is signed in, and autologin is enabled, it tries to sign in with the saved email and password.
     */
    private fun checkForUser() {
        viewModelScope.launch {
            // If there is no Firebase user, nor jvm override, we'll check for auto-login
            if(PlatformFirebase?.auth?.currentUser == null
                && dataManager.overrideUser.value == null
                && settings.hasKey(SETTINGS_KEY_AUTO_EMAIL)
            ) {
                val email = settings.getString(SETTINGS_KEY_AUTO_EMAIL, "")
                val password = settings.getString(SETTINGS_KEY_AUTO_PASSWORD, "")

                if(email != "" && password != "") {
                    // auto-login has credentials, let's try them out
                    userOperationService.signInWithPassword(email, password)?.let { response ->
                        if(response.email.isNotBlank()) {
                            overrideCurrentUser(CloudUserHelper.fromUserResponse(response))
                        }
                    }
                }
            }
        }
    }
}