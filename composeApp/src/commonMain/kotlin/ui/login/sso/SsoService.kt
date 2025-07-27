package ui.login.sso

import data.io.base.BaseResponse
import org.koin.core.module.Module
import ui.login.LoginResultType

expect fun ssoServiceModule(): Module
expect class SsoService {

    /** Requests signup or sign in via Google account */
    suspend fun requestGoogleSignIn(homeserver: String, filterAuthorizedAccounts: Boolean): BaseResponse<AugmySsoResponse>

    /** Requests signup or sign in via Apple id */
    suspend fun requestAppleSignIn(): LoginResultType
}