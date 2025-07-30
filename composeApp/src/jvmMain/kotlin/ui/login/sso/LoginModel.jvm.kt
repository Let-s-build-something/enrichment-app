package ui.login.sso

import data.io.base.BaseResponse
import org.koin.dsl.module
import ui.login.LoginResultType

/** module providing platform-specific sign in options */
actual fun ssoServiceModule() = module {
    single<SsoService> { SsoService() }
}
actual class SsoService() {

    actual suspend fun requestGoogleSignIn(homeserver: String, filterAuthorizedAccounts: Boolean): BaseResponse<AugmySsoResponse> {
        return BaseResponse.Error()
    }

    actual suspend fun requestAppleSignIn(): LoginResultType {
        return LoginResultType.FAILURE
    }
}