package ui.login

import kotlinx.coroutines.flow.MutableStateFlow

class LoginDataManager {
    val loginHomeserverResponse = MutableStateFlow<LoginModel.HomeServerResponse?>(null)
    val registrationHomeserverResponse = MutableStateFlow<LoginModel.HomeServerResponse?>(null)
}