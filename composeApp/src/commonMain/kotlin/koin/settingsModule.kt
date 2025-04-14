@file:OptIn(ExperimentalSettingsApi::class)

package koin

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import org.koin.dsl.module

interface AppSettings: FlowSettings
interface SecureAppSettings: Settings {
    fun clear(force: Boolean)
}

internal val settingsModule = module {
    single<AppSettings> { settings }
    single<SecureAppSettings> { secureSettings }
}