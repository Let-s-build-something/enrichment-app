package database.factory

import data.io.app.SecureSettingsKeys.KEY_DB_KEY
import data.io.app.SecureSettingsKeys.SECRET_BYTE_ARRAY_KEY_KEY
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import koin.SecureAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.SecureRandom
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

private val log = KotlinLogging.logger {}

@Serializable
sealed interface SecretByteArray {
    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretByteArray
}

@Serializable
sealed interface SecretByteArrayKey {
    @Serializable
    @SerialName("aes-hmac-sha2")
    data class AesHmacSha2(
        val iv: String, // base64 encoded
        val ciphertext: String, // base64 encoded
        val mac: String // base64 encoded
    ) : SecretByteArrayKey

    /**
     * This is only needed when there is no secure way to store the key. This is not secure at all, but it allows us to
     * make it secure in future (for example when keyring support for linux is added).
     */
    @Serializable
    @SerialName("unencrypted")
    data class Unencrypted(val value: @Serializable(ByteArrayBase64Serializer::class) ByteArray) : SecretByteArrayKey {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Unencrypted

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }
}

interface ConvertSecretByteArray {
    suspend operator fun invoke(raw: ByteArray): SecretByteArray
    suspend operator fun invoke(secret: SecretByteArray): ByteArray
}

fun convertSecretByteArrayModule() = module {
    single<ConvertSecretByteArray> {
        val getSecretByteArrayKey = get<GetSecretByteArrayKey>()

        object : ConvertSecretByteArray {
            override suspend operator fun invoke(raw: ByteArray): SecretByteArray {
                val secretByteArrayKey = getSecretByteArrayKey(32)
                val encryptedStringSecret =
                    encryptAesHmacSha2(
                        content = raw,
                        key = secretByteArrayKey,
                        name = "secret"
                    )
                return SecretByteArray.AesHmacSha2(
                    iv = encryptedStringSecret.iv,
                    ciphertext = encryptedStringSecret.ciphertext,
                    mac = encryptedStringSecret.mac,
                )
            }

            override suspend operator fun invoke(secret: SecretByteArray): ByteArray =
                when (secret) {
                    is SecretByteArray.AesHmacSha2 -> {
                        val secretByteArrayKey = getSecretByteArrayKey(32)
                        decryptAesHmacSha2(
                            content = AesHmacSha2EncryptedData(
                                iv = secret.iv,
                                ciphertext = secret.ciphertext,
                                mac = secret.mac,
                            ),
                            key = secretByteArrayKey,
                            name = "secret"
                        )
                    }
                }
        }
    }
}

fun platformGetSecretByteArrayKey(): Module = module {
    single<GetSecretByteArrayKey> { GetSecretByteArrayKeyBase() }
}

interface GetSecretByteArrayKey {
    /**
     * @return null when not possible to create a key on this platform
     */
    suspend operator fun invoke(sizeOnCreate: Int): ByteArray
}

class GetSecretByteArrayKeyBase: GetSecretByteArrayKey {
    private val secureSettings: SecureAppSettings by KoinPlatform.getKoin().inject()
    private val json: Json by KoinPlatform.getKoin().inject()

    private suspend fun getSecretByteArrayKeyKey(sizeOnCreate: Int): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val existingKey = secureSettings.getStringOrNull(SECRET_BYTE_ARRAY_KEY_KEY)?.decodeBase64Bytes()
            if (existingKey == null) {
                val newKey = SecureRandom.nextBytes(sizeOnCreate)
                secureSettings.putString(SECRET_BYTE_ARRAY_KEY_KEY, newKey.encodeBase64())
                newKey
            }else existingKey
        } catch (exc: Exception) {
            log.error(exc) { "Cannot read or set secret ('$SECRET_BYTE_ARRAY_KEY_KEY')." }
            null
        }
    }

    private suspend fun getSecretByteArrayKeyFromSettings() = withContext(Dispatchers.IO) {
        secureSettings.getStringOrNull(KEY_DB_KEY)?.let {
            json.decodeFromString<SecretByteArrayKey>(it)
        }
    }

    private suspend fun setSecretByteArrayKeyInSettings(
        secretByteArrayKey: SecretByteArrayKey?
    ) = withContext(Dispatchers.IO) {
        secureSettings.putString(
            key = KEY_DB_KEY,
            json.encodeToString(secretByteArrayKey)
        )
    }

    private val mutex = Mutex()
    override suspend fun invoke(sizeOnCreate: Int): ByteArray = mutex.withLock {
        val existing = getSecretByteArrayKeyFromSettings()

        if (existing != null) {
            try {
                convert(existing, getSecretByteArrayKeyKey(sizeOnCreate))
            }catch (e: Exception) {
                createKey(sizeOnCreate)
            }
        } else {
            createKey(sizeOnCreate)
        }
    }

    private suspend fun createKey(sizeOnCreate: Int): ByteArray {
        log.debug { "there is no SecretByteArrayKey yet, generate new one" }
        val newKey = SecureRandom.nextBytes(sizeOnCreate)
        val secretByteArrayKey = convert(newKey, getSecretByteArrayKeyKey(sizeOnCreate))
        setSecretByteArrayKeyInSettings(secretByteArrayKey)
        return newKey
    }

    private suspend fun convert(
        secretByteArrayKey: SecretByteArrayKey,
        secretByteArrayKeyKey: ByteArray?,
    ): ByteArray =
        when (secretByteArrayKey) {
            is SecretByteArrayKey.AesHmacSha2 -> {
                requireNotNull(secretByteArrayKeyKey) { "could not find key for SecretByteArrayKey" }
                decryptAesHmacSha2(
                    content = AesHmacSha2EncryptedData(
                        iv = secretByteArrayKey.iv,
                        ciphertext = secretByteArrayKey.ciphertext,
                        mac = secretByteArrayKey.mac,
                    ),
                    key = secretByteArrayKeyKey,
                    name = "secret"
                )
            }

            is SecretByteArrayKey.Unencrypted -> secretByteArrayKey.value
        }

    private suspend fun convert(
        raw: ByteArray,
        secretByteArrayKeyKey: ByteArray?,
    ): SecretByteArrayKey =
        if (secretByteArrayKeyKey != null) {
            val encryptedStringSecret =
                encryptAesHmacSha2(
                    content = raw,
                    key = secretByteArrayKeyKey,
                    name = "secret"
                )
            SecretByteArrayKey.AesHmacSha2(
                iv = encryptedStringSecret.iv,
                ciphertext = encryptedStringSecret.ciphertext,
                mac = encryptedStringSecret.mac,
            )
        } else SecretByteArrayKey.Unencrypted(raw)
}
