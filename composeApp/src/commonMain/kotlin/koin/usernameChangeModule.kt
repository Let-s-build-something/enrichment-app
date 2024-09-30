package koin

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.LoginViewModel
import ui.login.signInServiceModule
import ui.login.username.UsernameChangeViewModel

internal val usernameChangeModule = module {
    viewModelOf(::UsernameChangeViewModel)
}