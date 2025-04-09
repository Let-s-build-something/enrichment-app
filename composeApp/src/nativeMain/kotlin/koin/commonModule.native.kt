package koin

import augmy.interactive.com.BuildKonfig
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import data.io.app.SecureSettingsKeys.persistentKeys
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalSettingsApi::class)
actual val settings: AppSettings = object : AppSettings, FlowSettings by NSUserDefaultsSettings(
    delegate = NSUserDefaults(suiteName = if(BuildKonfig.isDevelopment) "app_preferences_dev" else "app_preferences")
).toFlowSettings() {}

@OptIn(ExperimentalSettingsImplementation::class)
actual val secureSettings: SecureAppSettings = object : SecureAppSettings, Settings by KeychainSettings(
    service = if(BuildKonfig.isDevelopment) "secure_app_preferences_dev" else "secure_app_preferences"
) {

    override fun clear() {
        keys.forEach { key ->
            if(persistentKeys.none { it == key || key.contains(it) }) remove(key)
        }
    }
}