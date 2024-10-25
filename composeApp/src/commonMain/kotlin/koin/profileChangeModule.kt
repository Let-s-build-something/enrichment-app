package koin

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.account.profile.UsernameChangeRepository
import ui.account.profile.ProfileChangeViewModel

internal val profileChangeModule = module {
    factory {
        UsernameChangeRepository(get<HttpClient>())
    }
    viewModelOf(::ProfileChangeViewModel)
}