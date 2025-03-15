package koin

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import data.io.app.SecureSettingsKeys.persistentKeys
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual val settings: AppSettings = object : AppSettings, FlowSettings by DataStoreSettings(
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            getKoin().get<Context>().preferencesDataStoreFile("app_preferences").absolutePath.toPath()
        }
    )
) {}

actual val secureSettings: SecureAppSettings = object : SecureAppSettings, ObservableSettings by SharedPreferencesSettings(
    EncryptedSharedPreferences.create(
        getKoin().get<Context>(),
        "secure_app_prefs",
        MasterKey.Builder(
            getKoin().get<Context>()
        ).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
) {

    override fun clear() {
        keys.forEach { key ->
            if(persistentKeys.none { it.contains(key) }) remove(key)
        }
    }
}
