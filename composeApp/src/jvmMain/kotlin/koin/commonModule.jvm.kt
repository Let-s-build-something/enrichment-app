package koin

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import augmy.interactive.com.BuildKonfig
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import data.io.app.SecureSettingsKeys.persistentKeys
import okio.Path.Companion.toPath
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.prefs.Preferences
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@OptIn(ExperimentalSettingsImplementation::class, ExperimentalSettingsApi::class)
actual val settings: AppSettings = object : AppSettings, FlowSettings by DataStoreSettings(PreferenceDataStoreFactory.createWithPath(
    produceFile = {
        File(
             System.getProperty("user.dir")
                    + File.separator
                    + "datastore"
                    + File.separator + if(BuildKonfig.isDevelopment) {
                        "app_preferences_dev.preferences_pb"
             } else "app_preferences.preferences_pb"
        ).absolutePath.toPath()
    }
)) {}

actual val secureSettings: SecureAppSettings = object : SecureAppSettings {
    private val prefs: Preferences = Preferences.userNodeForPackage(SecureAppSettings::class.java)
    private var _secretKey: SecretKey? = null
    private val secretKey: SecretKey
        get() {
            return (if(_secretKey == null) {
                generateOrLoadKey().also {
                    _secretKey = it
                }
            }else _secretKey) ?: generateOrLoadKey()
        }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = getStringOrNull(key)?.toBoolean()
    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = getStringOrNull(key)?.toDoubleOrNull()
    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = getStringOrNull(key)?.toFloatOrNull()
    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = getStringOrNull(key)?.toIntOrNull()
    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = getStringOrNull(key)?.toLongOrNull()
    override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? {
        val encryptedData = prefs.get(key, null) ?: return null
        return decrypt(encryptedData, secretKey)
    }


    override fun hasKey(key: String): Boolean = prefs.get(key, null) != null
    override fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())
    override fun putDouble(key: String, value: Double) = putString(key, value.toString())
    override fun putFloat(key: String, value: Float) = putString(key, value.toString())
    override fun putInt(key: String, value: Int) = putString(key, value.toString())
    override fun putLong(key: String, value: Long) = putString(key, value.toString())

    override fun putString(key: String, value: String) {
        val encryptedData = encrypt(value, secretKey)
        prefs.put(key, encryptedData)
    }

    override fun remove(key: String) {
        prefs.remove(key)
    }

    override val keys: Set<String>
        get() = prefs.keys().toSet()

    override val size: Int
        get() = keys.size

    override fun clear() {
        keys.forEach { key ->
            if(persistentKeys.none { it.contains(key) }) remove(key)
        }
        _secretKey = null
    }

    private fun encrypt(plainText: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        val cipherText = cipher.doFinal(plainText.toByteArray())
        return Base64.getEncoder().encodeToString(iv + cipherText)
    }

    private fun decrypt(encryptedData: String, key: SecretKey): String {
        return try {
            val decodedData = Base64.getDecoder().decode(encryptedData)
            val iv = decodedData.copyOfRange(0, 12)
            val cipherText = decodedData.copyOfRange(12, decodedData.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText))
        } catch (e: AEADBadTagException) {
            prefs.clear()
            _secretKey = null
            decrypt(encryptedData, key)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun generateOrLoadKey(): SecretKey {
        val keyName = if(BuildKonfig.isDevelopment) "SecureDevAppKey" else "SecureAppKey"
        val storedKey = prefs.get(keyName, null)
        return if (storedKey != null) {
            try {
                val decodedKey = Base64.getDecoder().decode(storedKey)
                SecretKeySpec(decodedKey, "AES")
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalStateException("Failed to decode stored key")
            }
        } else {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val newKey = keyGen.generateKey()
            prefs.put(keyName, Base64.getEncoder().encodeToString(newKey.encoded))
            newKey
        }
    }
}
