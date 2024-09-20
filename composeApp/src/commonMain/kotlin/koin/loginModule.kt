package koin

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.LoginRepository
import ui.login.LoginViewModel

internal fun loginModule() = module {
    factory { LoginRepository(get()) }
    viewModelOf(::LoginViewModel)
}