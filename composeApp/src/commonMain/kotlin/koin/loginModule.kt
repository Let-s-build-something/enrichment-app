package koin

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.LoginModel
import ui.login.LoginRepository
import ui.login.signInServiceModule

internal fun loginModule() = module {
    includes(signInServiceModule())
    factory { LoginRepository(get()) }
    viewModelOf(::LoginModel)
}