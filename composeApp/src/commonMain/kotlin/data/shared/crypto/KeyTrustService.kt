package data.shared.crypto

import data.shared.crypto.model.KeyChainLink
import data.shared.crypto.model.KeySignatureTrustLevel
import data.shared.crypto.model.KeySignatureTrustLevel.Blocked
import data.shared.crypto.model.KeySignatureTrustLevel.CrossSigned
import data.shared.crypto.model.KeySignatureTrustLevel.Invalid
import data.shared.crypto.model.KeySignatureTrustLevel.NotCrossSigned
import data.shared.crypto.model.KeySignatureTrustLevel.Valid
import data.shared.crypto.model.KeyVerificationState
import data.shared.crypto.model.StoredSecret
import data.shared.crypto.verification.ActiveDeviceVerification
import data.shared.crypto.verification.ActiveDeviceVerificationImpl
import data.shared.crypto.verification.SelfVerificationMethod
import data.shared.crypto.verification.SelfVerificationMethods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.MegolmBackupV1EventContent
import net.folivo.trixnity.core.model.events.m.crosssigning.MasterKeyEventContent
import net.folivo.trixnity.core.model.events.m.crosssigning.SelfSigningKeyEventContent
import net.folivo.trixnity.core.model.events.m.crosssigning.UserSigningKeyEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod.Sas
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.DefaultSecretKeyEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent.AesHmacSha2Key
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage.MasterKey
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage.SelfSigningKey
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage.UserSigningKey
import net.folivo.trixnity.core.model.keys.DeviceKeys
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.Key.Ed25519Key
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys
import net.folivo.trixnity.crypto.SecretType
import net.folivo.trixnity.crypto.SecretType.M_CROSS_SIGNING_SELF_SIGNING
import net.folivo.trixnity.crypto.SecretType.M_CROSS_SIGNING_USER_SIGNING
import net.folivo.trixnity.crypto.core.SecureRandom
import net.folivo.trixnity.crypto.key.decryptSecret
import net.folivo.trixnity.crypto.olm.OlmDecrypter
import net.folivo.trixnity.crypto.olm.OlmEncryptionService
import net.folivo.trixnity.crypto.sign.SignService
import net.folivo.trixnity.crypto.sign.SignWith
import net.folivo.trixnity.crypto.sign.VerifyResult
import net.folivo.trixnity.crypto.sign.sign
import net.folivo.trixnity.crypto.sign.verify
import net.folivo.trixnity.olm.OlmPkSigning
import net.folivo.trixnity.olm.freeAfter
import net.folivo.trixnity.utils.decodeUnpaddedBase64Bytes
import net.folivo.trixnity.utils.nextString
import kotlin.jvm.JvmName

typealias Signatures<T> = Map<T, Keys>

class KeyTrustService(
    private val signService: SignService,
    private val userInfo: UserInfo,
    private val encryptionService: OlmEncryptionService,
    private val json: Json,
    private val olmDecrypter: OlmDecrypter,
    private val clock: Clock,
    private val keyStore: OlmCryptoStore,
    private val repository: EncryptionServiceRepository
) {
    private val _activeDeviceVerification = MutableStateFlow<ActiveDeviceVerificationImpl?>(null)
    val activeDeviceVerification = _activeDeviceVerification.asStateFlow()
    private val supportedMethods: Set<VerificationMethod> = setOf(Sas)
    private val ownUserId = userInfo.userId
    private val ownDeviceId = userInfo.deviceId

    /*TODO maintain lifecycle
       fun startInCoroutineScope(scope: CoroutineScope) {
        api.sync.subscribeContent(subscriber = ::handleDeviceVerificationRequestEvents)
            .unsubscribeOnCompletion(scope)
        olmDecrypter.subscribe(::handleOlmDecryptedDeviceVerificationRequestEvents)
            .unsubscribeOnCompletion(scope)
        // we use UNDISPATCHED because we want to ensure, that collect is called immediately
        scope.launch(start = UNDISPATCHED) {
            activeUserVerifications.collect { startLifecycleOfActiveVerifications(it, this) }
        }
        scope.launch(start = UNDISPATCHED) {
            activeDeviceVerification.collect { it?.let { startLifecycleOfActiveVerifications(listOf(it), this) } }
        }
    }*/

    suspend fun calculateDeviceKeysTrustLevel(deviceKeys: SignedDeviceKeys): KeySignatureTrustLevel {
        println( "calculate trust level for ${deviceKeys.signed}")
        val userId = deviceKeys.signed.userId
        val signedKey = deviceKeys.signed.keys.get<Ed25519Key>()
            ?: return Invalid("missing ed25519 key")
        return calculateTrustLevel(
            userId,
            { signService.verify(deviceKeys, it) },
            signedKey,
            deviceKeys.signatures ?: mapOf(),
            deviceKeys.getVerificationState(),
            false
        ).also { println( "calculated trust level of ${deviceKeys.signed} from $userId is $it") }
    }

    suspend fun calculateCrossSigningKeysTrustLevel(crossSigningKeys: SignedCrossSigningKeys): KeySignatureTrustLevel {
        println( "calculate trust level for ${crossSigningKeys.signed}")
        val userId = crossSigningKeys.signed.userId
        val signedKey = crossSigningKeys.signed.keys.get<Ed25519Key>()
            ?: return Invalid("missing ed25519 key")
        return calculateTrustLevel(
            userId,
            { signService.verify(crossSigningKeys, it) },
            signedKey,
            crossSigningKeys.signatures ?: mapOf(),
            crossSigningKeys.getVerificationState(),
            crossSigningKeys.signed.usage.contains(MasterKey)
        ).also { println( "calculated trust level of ${crossSigningKeys.signed} from $userId is $it") }
    }

    private suspend fun calculateTrustLevel(
        userId: UserId,
        verifySignedObject: suspend (signingKeys: Map<UserId, Set<Ed25519Key>>) -> VerifyResult,
        signedKey: Ed25519Key,
        signatures: Signatures<UserId>,
        keyVerificationState: KeyVerificationState?,
        isMasterKey: Boolean
    ): KeySignatureTrustLevel {
        val masterKey = keyStore.getCrossSigningKey(userId, MasterKey)
        return when {
            keyVerificationState is KeyVerificationState.Verified && isMasterKey -> CrossSigned(true)
            keyVerificationState is KeyVerificationState.Verified && (masterKey == null) -> Valid(true)
            keyVerificationState is KeyVerificationState.Blocked -> Blocked
            else -> searchSignaturesForTrustLevel(userId, verifySignedObject, signedKey, signatures)
                ?: when {
                    isMasterKey -> CrossSigned(false)
                    else -> if (masterKey == null) Valid(false) else NotCrossSigned
                }
        }
    }

    private suspend fun searchSignaturesForTrustLevel(
        signedUserId: UserId,
        verifySignedObject: suspend (signingKeys: Map<UserId, Set<Ed25519Key>>) -> VerifyResult,
        signedKey: Ed25519Key,
        signatures: Signatures<UserId>,
        visitedKeys: MutableSet<Pair<UserId, String?>> = mutableSetOf()
    ): KeySignatureTrustLevel? {
        println("search in signatures of $signedKey for trust level calculation: $signatures")
        visitedKeys.add(signedUserId to signedKey.keyId)
        keyStore.deleteKeyChainLinksBySignedKey(signedUserId, signedKey)
        val states = signatures.flatMap { (signingUserId, signatureKeys) ->
            signatureKeys
                .filterIsInstance<Ed25519Key>()
                .filterNot { visitedKeys.contains(signingUserId to it.keyId) }
                .flatMap { signatureKey ->
                    visitedKeys.add(signingUserId to signatureKey.keyId)

                    val crossSigningKey =
                        signatureKey.keyId?.let { keyStore.getCrossSigningKey(signingUserId, it) }?.value
                    val signingCrossSigningKey = crossSigningKey?.signed?.get<Ed25519Key>()
                    val crossSigningKeyState = if (signingCrossSigningKey != null) {
                        val isValid = verifySignedObject(mapOf(signingUserId to setOf(signingCrossSigningKey)))
                            .also { v ->
                                if (v != VerifyResult.Valid)
                                    println("signature was $v for key chain $signingCrossSigningKey ($signingUserId) ---> $signedKey ($signedUserId)")
                            } == VerifyResult.Valid
                        if (isValid) when (crossSigningKey.getVerificationState()) {
                            is KeyVerificationState.Verified -> CrossSigned(true)
                            is KeyVerificationState.Blocked -> Blocked
                            else -> {
                                searchSignaturesForTrustLevel(
                                    signingUserId,
                                    { signService.verify(crossSigningKey, it) },
                                    signingCrossSigningKey,
                                    crossSigningKey.signatures ?: mapOf(),
                                    visitedKeys
                                ) ?: if (crossSigningKey.signed.usage.contains(MasterKey)
                                    && crossSigningKey.signed.userId == signedUserId
                                    && crossSigningKey.signed.userId == signingUserId
                                ) CrossSigned(false) else null
                            }
                        } else null
                    } else null

                    val deviceKey = signatureKey.keyId?.let { keyStore.getDeviceKey(signingUserId.full, it) }?.value
                    val signingDeviceKey = deviceKey?.get<Ed25519Key>()
                    val deviceKeyState = if (signingDeviceKey != null) {
                        val isValid = verifySignedObject(mapOf(signingUserId to setOf(signingDeviceKey)))
                            .also { v ->
                                if (v != VerifyResult.Valid)
                                    println("signature was $v for key chain $signingCrossSigningKey ($signingUserId) ---> $signedKey ($signedUserId)")
                            } == VerifyResult.Valid
                        if (isValid) when (deviceKey.getVerificationState()) {
                            is KeyVerificationState.Verified -> CrossSigned(true)
                            is KeyVerificationState.Blocked -> Blocked
                            else -> searchSignaturesForTrustLevel(
                                signedUserId,
                                { signService.verify(deviceKey, it) },
                                signingDeviceKey,
                                deviceKey.signatures ?: mapOf(),
                                visitedKeys
                            )
                        } else null
                    } else null

                    val signingKey = signingCrossSigningKey ?: signingDeviceKey
                    if (signingKey != null) {
                        keyStore.saveKeyChainLink(KeyChainLink(signingUserId, signingKey, signedUserId, signedKey))
                    }

                    listOf(crossSigningKeyState, deviceKeyState)
                }.toSet()
        }.toSet()
        return when {
            states.any { it is CrossSigned && it.verified } -> CrossSigned(true)
            states.any { it is CrossSigned && !it.verified } -> CrossSigned(false)
            states.contains(Blocked) -> Blocked
            else -> null
        }
    }

    suspend fun updateTrustLevelOfKeyChainSignedBy(
        signingUserId: UserId,
        signingKey: Ed25519Key,
        visitedKeys: MutableSet<Pair<UserId, String?>> = mutableSetOf()
    ) {
        println("update trust level of all keys signed by $signingUserId $signingKey")
        visitedKeys.add(signingUserId to signingKey.keyId)
        keyStore.getKeyChainLinksBySigningKey(signingUserId, signingKey)
            .filterNot { visitedKeys.contains(it.signedUserId to it.signedKey.keyId) }
            .forEach { keyChainLink ->
                updateTrustLevelOfKey(keyChainLink.signedUserId, keyChainLink.signedKey)
                updateTrustLevelOfKeyChainSignedBy(keyChainLink.signedUserId, keyChainLink.signedKey, visitedKeys)
            }
    }

    private suspend fun updateTrustLevelOfKey(userId: UserId, key: Ed25519Key) {
        val keyId = key.keyId

        if (keyId != null) {
            val foundKey = MutableStateFlow(false)

            keyStore.updateDeviceKeys(userId) { oldDeviceKeys ->
                val foundDeviceKeys = oldDeviceKeys?.get(keyId)
                if (foundDeviceKeys != null) {
                    val newTrustLevel = calculateDeviceKeysTrustLevel(foundDeviceKeys.value)
                    foundKey.value = true
                    println("updated device keys ${foundDeviceKeys.value.signed.deviceId} of user $userId with trust level $newTrustLevel")
                    oldDeviceKeys + (keyId to foundDeviceKeys.copy(trustLevel = newTrustLevel))
                } else oldDeviceKeys
            }
            if (foundKey.value.not()) {
                keyStore.updateCrossSigningKeys(userId) { oldKeys ->
                    val foundCrossSigningKeys = oldKeys?.firstOrNull { keys ->
                        keys.value.signed.keys.keys.filterIsInstance<Ed25519Key>().any { it.keyId == keyId }
                    }
                    if (foundCrossSigningKeys != null) {
                        val newTrustLevel = calculateCrossSigningKeysTrustLevel(foundCrossSigningKeys.value)
                        foundKey.value = true
                        println("updated cross signing key ${foundCrossSigningKeys.value.signed.usage.firstOrNull()?.name} of user $userId with trust level $newTrustLevel")
                        (oldKeys - foundCrossSigningKeys) + foundCrossSigningKeys.copy(trustLevel = newTrustLevel)

                    } else oldKeys
                }
            }
            if (foundKey.value.not()) println("could not find device or cross signing keys of $key")
        } else println("could not update trust level, because key id of $key was null")
    }

    suspend fun trustAndSignKeys(keys: Set<Ed25519Key>, userId: UserId) {
        println("sign keys (when possible): $keys")
        val signedDeviceKeys = keys.mapNotNull { key ->
            val deviceKey = key.keyId?.let { keyStore.getDeviceKey(userId.full, it) }?.value?.signed
            if (deviceKey != null) {
                keyStore.saveKeyVerificationState(key, KeyVerificationState.Verified(key.value))
                updateTrustLevelOfKey(userId, key)
                try {
                    if (userId == userInfo.userId && deviceKey.get<Ed25519Key>() == key) {
                        signService.sign(deviceKey, signWithSecret(M_CROSS_SIGNING_SELF_SIGNING))
                            .also { println( "signed own accounts device with own self signing key") }
                    } else null
                } catch (error: Exception) {
                    println("could not sign device key $key with self signing key: ${error.message}")
                    null
                }
            } else null
        }
        val signedCrossSigningKeys = keys.mapNotNull { key ->
            val crossSigningKey = key.keyId?.let { keyStore.getCrossSigningKey(userId, it) }?.value?.signed
            if (crossSigningKey != null) {
                keyStore.saveKeyVerificationState(key, KeyVerificationState.Verified(key.value))
                updateTrustLevelOfKey(userId, key)
                if (crossSigningKey.usage.contains(MasterKey)) {
                    if (crossSigningKey.get<Ed25519Key>() == key) {
                        if (userId == userInfo.userId) {
                            try {
                                signService.sign(crossSigningKey, SignWith.DeviceKey)
                                    .also { println("signed own master key with own device key") }
                            } catch (error: Exception) {
                                println("could not sign own master key $key with device key: ${error.message}")
                                null
                            }
                        } else {
                            try {
                                signService.sign(crossSigningKey, signWithSecret(
                                    M_CROSS_SIGNING_USER_SIGNING
                                ))
                                    .also { println("signed other users master key with own user signing key") }
                            } catch (error: Exception) {
                                println("could not sign other users master key $key with user signing key: ${error.message}")
                                null
                            }
                        }
                    } else null
                } else null
            } else null
        }
        if (signedDeviceKeys.isNotEmpty() || signedCrossSigningKeys.isNotEmpty()) {
            println("upload signed keys: ${signedDeviceKeys + signedCrossSigningKeys}")
            val response = repository.addSignatures(
                signedDeviceKeys.toSet(),
                signedCrossSigningKeys.toSet()
            )
            if (!response?.failures.isNullOrEmpty()) {
                println("could not add signatures to server: ${response?.failures}")
                throw UploadSignaturesException(response?.failures.toString())
            }
        }
    }

    private suspend fun signWithSecret(type: SecretType): SignWith.PrivateKey {
        val privateKey = keyStore.getSecrets()[type]?.decryptedPrivateKey
        requireNotNull(privateKey) { "could not find private key of $type" }
        val publicKey =
            keyStore.getCrossSigningKey(
                userInfo.userId,
                when (type) {
                    M_CROSS_SIGNING_SELF_SIGNING -> SelfSigningKey
                    M_CROSS_SIGNING_USER_SIGNING -> UserSigningKey
                    else -> throw IllegalArgumentException("cannot sign with $type")
                }
            )?.value?.signed?.get<Ed25519Key>()?.keyId
        requireNotNull(publicKey) { "could not find public key of $type" }
        return SignWith.PrivateKey(privateKey, publicKey)
    }

    suspend fun getSelfVerificationMethods(): SelfVerificationMethods {
        val crossSigningKeys = keyStore.getCrossSigningKeys(ownUserId)
        val deviceKeys = keyStore.getDeviceKeys(ownUserId.full).takeIf { it.isNotEmpty() }
        val defaultKey = keyStore.getSecretKeyEvent<DefaultSecretKeyEventContent>()?.let {
            keyStore.getSecretKeyEvent<SecretKeyEventContent>(it.key)
        }

        // preconditions: sync running, login was successful and we are not yet cross-signed
        if (deviceKeys == null || crossSigningKeys == null)
            return SelfVerificationMethods.PreconditionsNotMet(
                setOfNotNull(
                    if (deviceKeys == null) SelfVerificationMethods.PreconditionsNotMet.Reason.DeviceKeysNotFetchedYet else null,
                    if (crossSigningKeys == null) SelfVerificationMethods.PreconditionsNotMet.Reason.CrossSigningKeysNotFetchedYet else null
                )
            )
        val ownTrustLevel = deviceKeys[ownDeviceId]?.trustLevel
        if (ownTrustLevel == CrossSigned(true)) return SelfVerificationMethods.AlreadyCrossSigned

        // we need bootstrapping if this is the first device or bootstrapping is in progress
        if (crossSigningKeys.isEmpty()) return SelfVerificationMethods.NoCrossSigningEnabled

        val deviceVerificationMethod = deviceKeys.entries
            .filter { it.value.trustLevel is CrossSigned }
            .map { it.key }
            .let {
                val sendToDevices = it - ownDeviceId
                if (sendToDevices.isNotEmpty())
                    setOf(
                        SelfVerificationMethod.CrossSignedDeviceVerification(
                            ownUserId,
                            sendToDevices.toSet(),
                            ::createDeviceVerificationRequest
                        )
                    )
                else setOf()
            }

        val recoveryKeyMethods = when (val content = defaultKey?.content) {
            is AesHmacSha2Key -> when (content.passphrase) {
                is AesHmacSha2Key.SecretStorageKeyPassphrase.Pbkdf2 ->
                    setOf(
                        SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase(
                            this,
                            defaultKey.key,
                            content
                        ),
                        SelfVerificationMethod.AesHmacSha2RecoveryKey(
                            this,
                            defaultKey.key,
                            content
                        )
                    )

                is AesHmacSha2Key.SecretStorageKeyPassphrase.Unknown, null ->
                    setOf(
                        SelfVerificationMethod.AesHmacSha2RecoveryKey(
                            this,
                            defaultKey.key,
                            content
                        )
                    )
            }

            is SecretKeyEventContent.Unknown, null -> setOf()
        }

        return SelfVerificationMethods.CrossSigningEnabled(recoveryKeyMethods + deviceVerificationMethod)
    }

    suspend fun createDeviceVerificationRequest(
        theirUserId: UserId,
        theirDeviceIds: Set<String>
    ): Result<ActiveDeviceVerification> = kotlin.runCatching {
        println("create new device verification request to $theirUserId ($theirDeviceIds)")
        val request = VerificationRequestToDeviceEventContent(
            ownDeviceId, supportedMethods, clock.now().toEpochMilliseconds(), SecureRandom.nextString(22)
        )
        repository.sendToDevice(mapOf(theirUserId to theirDeviceIds.toSet().associateWith {
            encryptionService.encryptOlm(request, theirUserId, it).getOrNull() ?: request
        })).getOrThrow()
        ActiveDeviceVerificationImpl(
            request = request,
            requestIsOurs = true,
            ownUserId = ownUserId,
            ownDeviceId = ownDeviceId,
            theirUserId = theirUserId,
            theirDeviceIds = theirDeviceIds.toSet(),
            supportedMethods = supportedMethods,
            olmDecrypter = olmDecrypter,
            olmEncryptionService = encryptionService,
            keyTrust = this,
            keyStore = keyStore,
            clock = clock,
            json = json,
            repository = repository
        ).also { newDeviceVerification ->
            _activeDeviceVerification.getAndUpdate { newDeviceVerification }?.cancel()
        }
    }

    suspend fun checkOwnAdvertisedMasterKeyAndVerifySelf(
        key: ByteArray,
        keyId: String,
        keyInfo: SecretKeyEventContent
    ): Result<Unit> {
        val encryptedMasterKey = keyStore.getSecretKeyEvent<MasterKeyEventContent>()?.content
            ?: return Result.failure(MasterKeyInvalidException("could not find encrypted master key"))
        val decryptedPublicKey =
            kotlin.runCatching {
                decryptSecret(key, keyId, keyInfo, "m.cross_signing.master", encryptedMasterKey, json)
            }.getOrNull()
                ?.let { privateKey ->
                    freeAfter(OlmPkSigning.create(privateKey)) { it.publicKey }
                }
        val advertisedPublicKey =
            keyStore.getCrossSigningKey(userInfo.userId, MasterKey)?.value?.signed?.get<Ed25519Key>()
        return if (advertisedPublicKey?.value?.decodeUnpaddedBase64Bytes()
                ?.contentEquals(decryptedPublicKey?.decodeUnpaddedBase64Bytes()) == true
        ) {
            val ownDeviceKeys =
                keyStore.getDeviceKey(userInfo.userId.full, userInfo.deviceId)?.value?.get<Ed25519Key>()
            kotlin.runCatching {
                trustAndSignKeys(setOfNotNull(advertisedPublicKey, ownDeviceKeys), userInfo.userId)
            }
        } else Result.failure(MasterKeyInvalidException("master public key $decryptedPublicKey did not match the advertised ${advertisedPublicKey?.value}"))
    }

    suspend fun decryptMissingSecrets(
        key: ByteArray,
        keyId: String,
        keyInfo: SecretKeyEventContent,
    ) {
        val decryptedSecrets = SecretType.entries
            .subtract(keyStore.getSecrets().keys)
            .mapNotNull { allowedSecret ->
                val event = allowedSecret.getEncryptedSecret(keyStore)
                if (event != null) {
                    kotlin.runCatching {
                        decryptSecret(key, keyId, keyInfo, allowedSecret.id, event.content, json)
                    }.getOrNull()
                        ?.let { allowedSecret to StoredSecret(event, it) }
                } else {
                    println("could not find secret ${allowedSecret.id} to decrypt and cache")
                    null
                }
            }.toMap()
        keyStore.updateSecrets {
            it + decryptedSecrets
        }
    }

    @JvmName("getVerificationStateCsk")
    private suspend fun SignedCrossSigningKeys.getVerificationState() =
        this.signed.keys.getVerificationState()

    @JvmName("getVerificationStateDk")
    private suspend fun SignedDeviceKeys.getVerificationState() =
        this.signed.keys.getVerificationState()

    private suspend fun Keys.getVerificationState() =
        this.firstNotNullOfOrNull { keyStore.getKeyVerificationState(it) }
}

class UploadSignaturesException(message: String) : RuntimeException(message)
class MasterKeyInvalidException(message: String) : RuntimeException(message)

internal inline fun <reified T : Key> DeviceKeys.get(): T? {
    return keys.keys.filterIsInstance<T>().firstOrNull()
}

internal suspend fun SecretType.getEncryptedSecret(keyStore: OlmCryptoStore) = when (this) {
    M_CROSS_SIGNING_USER_SIGNING -> keyStore.getSecretKeyEvent<UserSigningKeyEventContent>()
    M_CROSS_SIGNING_SELF_SIGNING -> keyStore.getSecretKeyEvent<SelfSigningKeyEventContent>()
    SecretType.M_MEGOLM_BACKUP_V1 -> keyStore.getSecretKeyEvent<MegolmBackupV1EventContent>()
}

internal inline fun <reified T : Key> SignedDeviceKeys.get(): T? {
    return signed.keys.keys.filterIsInstance<T>().firstOrNull()
}
