package koin

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import okio.Path.Companion.toPath
import java.io.File

@OptIn(ExperimentalSettingsImplementation::class, ExperimentalSettingsApi::class)
actual val settings: AppSettings = object : AppSettings, FlowSettings by DataStoreSettings(PreferenceDataStoreFactory.createWithPath(
    produceFile = {
        File(
             System.getProperty("user.dir")
                    + File.separator
                    + "datastore"
                    + File.separator + "app_preferences.preferences_pb"
        ).absolutePath.toPath()
    }
)) {}

@ExperimentalSettingsApi
actual val secureSettings: SecureAppSettings = object : SecureAppSettings {
    private var instance: PasswordSafe? = null
    private val passwordSafe: PasswordSafe?
        get() {
            if (instance == null) instance = PasswordSafe.instance
            return instance
        }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getBooleanOrNull(key) ?: defaultValue
    }

    override fun getBooleanOrNull(key: String): Boolean? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = passwordSafe?.get(credentialAttributes)
        return credentials?.getPasswordAsString()?.toBoolean()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return getDoubleOrNull(key) ?: defaultValue
    }

    override fun getDoubleOrNull(key: String): Double? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = passwordSafe?.get(credentialAttributes)
        return credentials?.getPasswordAsString()?.toDoubleOrNull()
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return getFloatOrNull(key) ?: defaultValue
    }

    override fun getFloatOrNull(key: String): Float? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = passwordSafe?.get(credentialAttributes)
        return credentials?.getPasswordAsString()?.toFloatOrNull()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getIntOrNull(key) ?: defaultValue
    }

    override fun getIntOrNull(key: String): Int? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = passwordSafe?.get(credentialAttributes)
        return credentials?.getPasswordAsString()?.toIntOrNull()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return getLongOrNull(key) ?: defaultValue
    }

    override fun getLongOrNull(key: String): Long? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = passwordSafe?.get(credentialAttributes)
        return credentials?.getPasswordAsString()?.toLongOrNull()
    }

    override fun getString(key: String, defaultValue: String): String {
        return getStringOrNull(key) ?: defaultValue
    }

    override fun getStringOrNull(key: String): String? {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = passwordSafe?.get(credentialAttributes)
        return credentials?.getPasswordAsString()
    }

    override fun hasKey(key: String): Boolean {
        return getStringOrNull(key) != null
    }

    override fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    override fun putDouble(key: String, value: Double) {
        putString(key, value.toString())
    }

    override fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    override fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    override fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    override fun putString(key: String, value: String) {
        val credentialAttributes = createCredentialAttributes(key)
        val credentials = Credentials(key, value)
        passwordSafe?.set(credentialAttributes, credentials)
    }

    override fun remove(key: String) {
        val credentialAttributes = createCredentialAttributes(key)
        passwordSafe?.set(credentialAttributes, null)
    }

    override val keys: Set<String>
        get() {
            // PasswordSafe does not support listing keys
            return emptySet()
        }

    override val size: Int
        get() {
            // PasswordSafe does not support counting keys
            return 0
        }

    override fun clear() {
        // Not supported by PasswordSafe
    }

    private fun createCredentialAttributes(key: String) = CredentialAttributes(
        serviceName = "AugmySecureAppSettings",
        userName = key
    )
}
