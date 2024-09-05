package koin

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.LoginViewModel
import ui.login.signInServiceModule

internal fun loginModule() = module {
    viewModelOf(::LoginViewModel)
}