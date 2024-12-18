package koin

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import org.koin.dsl.module

@OptIn(ExperimentalSettingsApi::class)
internal val settingsModule = module {
    single<FlowSettings> { settings }
}