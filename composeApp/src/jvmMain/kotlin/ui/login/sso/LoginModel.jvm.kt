package ui.login.sso

import data.io.base.BaseResponse
import org.koin.dsl.module
import ui.login.LoginResultType
import ui.login.sso.SsoServiceRepository.AugmySsoResponse

/** module providing platform-specific sign in options */
actual fun ssoServiceModule() = module {
    single<SsoService> { ui.login.sso.SsoService() }
}
actual class SsoService() {

    actual suspend fun requestGoogleSignIn(homeserver: String, filterAuthorizedAccounts: Boolean): BaseResponse<AugmySsoResponse> {
        return BaseResponse.Error()
    }

    actual suspend fun requestAppleSignIn(): LoginResultType {
        return LoginResultType.FAILURE
    }
}