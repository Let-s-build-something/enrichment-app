package koin

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.username.UsernameChangeRepository
import ui.login.username.UsernameChangeViewModel

internal val usernameChangeModule = module {
    factory {
        UsernameChangeRepository(get<HttpClient>())
    }
    viewModelOf(::UsernameChangeViewModel)
}