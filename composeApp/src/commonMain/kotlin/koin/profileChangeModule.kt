package koin

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.account.profile.DisplayNameChangeRepository
import ui.account.profile.ProfileChangeModel

internal val profileChangeModule = module {
    factory {
        DisplayNameChangeRepository(get<HttpClient>())
    }
    viewModelOf(::ProfileChangeModel)
}