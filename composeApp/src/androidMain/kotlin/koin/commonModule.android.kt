package koin

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import augmy.interactive.com.BuildKonfig
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import data.io.app.SecureSettingsKeys.persistentKeys
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin
import java.security.KeyStore
import javax.crypto.AEADBadTagException

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual val settings: AppSettings = object : AppSettings, FlowSettings by DataStoreSettings(
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            getKoin().get<Context>().preferencesDataStoreFile(
                if(BuildKonfig.isDevelopment) "app_preferences_dev" else "app_preferences"
            ).absolutePath.toPath()
        }
    )
) {}

actual val secureSettings: SecureAppSettings = object : SecureAppSettings, ObservableSettings by SharedPreferencesSettings(
    createEncryptedPrefs(
        context = getKoin().get<Context>(),
        keyAlias = (if(BuildKonfig.isDevelopment) "dev" else "prod") + "_security_master_key",
        prefsName = if(BuildKonfig.isDevelopment) "secure_preferences_dev" else "secure_preferences"
    )
) {

    override fun clear() {
        keys.forEach { key ->
            if(persistentKeys.none { it == key || key.contains(it) }) remove(key)
        }
    }
}

private fun createEncryptedPrefs(
    context: Context,
    keyAlias: String,
    prefsName: String
): SharedPreferences {
    val masterKey = MasterKey.Builder(context, keyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return try {
        EncryptedSharedPreferences.create(
            context,
            prefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: AEADBadTagException) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        keyStore.deleteEntry(keyAlias)

        createEncryptedPrefs(
            context = context,
            keyAlias = keyAlias,
            prefsName = prefsName
        )
    }
}
