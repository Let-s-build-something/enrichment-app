package ui.login

import kotlinx.coroutines.flow.MutableStateFlow

class LoginDataManager {
    val homeserverResponse = MutableStateFlow<LoginModel.HomeServerResponse?>(null)
}