package ui.login

import kotlinx.coroutines.flow.MutableStateFlow

class LoginDataManager {
    val homeServerResponse = MutableStateFlow<LoginModel.HomeServerResponse?>(null)
}