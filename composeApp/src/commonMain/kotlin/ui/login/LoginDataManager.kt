package ui.login

import kotlinx.coroutines.flow.MutableStateFlow

class LoginDataManager {
    var ssoNonce: String = "no nonce"
    val homeServerResponse = MutableStateFlow<LoginModel.HomeServerResponse?>(null)
}