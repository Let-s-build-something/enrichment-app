package koin

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalSettingsApi::class)
actual val settings: AppSettings = object : AppSettings, FlowSettings by NSUserDefaultsSettings(
    delegate = NSUserDefaults(suiteName = "app_preferences")
).toFlowSettings() {}

@OptIn(ExperimentalSettingsImplementation::class)
actual val secureSettings: SecureAppSettings = object : SecureAppSettings, Settings by KeychainSettings(
    service = "secure_app_preferences"
) {}