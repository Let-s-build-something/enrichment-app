package data.shared.crypto

import data.shared.crypto.model.BootstrapCrossSigning
import data.shared.crypto.model.StoredSecret
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.events.ClientEvent.GlobalAccountDataEvent
import net.folivo.trixnity.core.model.events.m.crosssigning.MasterKeyEventContent
import net.folivo.trixnity.core.model.events.m.crosssigning.SelfSigningKeyEventContent
import net.folivo.trixnity.core.model.events.m.crosssigning.UserSigningKeyEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.DefaultSecretKeyEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent
import net.folivo.trixnity.core.model.events.m.secretstorage.SecretKeyEventContent.AesHmacSha2Key
import net.folivo.trixnity.core.model.keys.CrossSigningKeys
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage.MasterKey
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage.SelfSigningKey
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage.UserSigningKey
import net.folivo.trixnity.core.model.keys.Key.Ed25519Key
import net.folivo.trixnity.core.model.keys.keysOf
import net.folivo.trixnity.crypto.SecretType.M_CROSS_SIGNING_SELF_SIGNING
import net.folivo.trixnity.crypto.SecretType.M_CROSS_SIGNING_USER_SIGNING
import net.folivo.trixnity.crypto.core.SecureRandom
import net.folivo.trixnity.crypto.core.createAesHmacSha2MacFromKey
import net.folivo.trixnity.crypto.key.encodeRecoveryKey
import net.folivo.trixnity.crypto.key.encryptSecret
import net.folivo.trixnity.crypto.sign.SignService
import net.folivo.trixnity.crypto.sign.SignWith
import net.folivo.trixnity.crypto.sign.sign
import net.folivo.trixnity.olm.OlmPkSigning
import net.folivo.trixnity.olm.freeAfter

class CrossSigningService(
    private val repository: EncryptionServiceRepository,
    private val signService: SignService,
    private val userInfo: UserInfo,
    private val json: Json,
    private val keyTrustService: KeyTrustService,
    private val keyStore: OlmCryptoStore,
    private val keyBackupService: KeyBackupService,
    private val keyHandler: OutdatedKeyHandler
) {

    private suspend fun bootstrapCrossSigning(
        recoveryKey: ByteArray,
        secretKeyEventContent: SecretKeyEventContent,
    ): BootstrapCrossSigning {
        println("bootstrap cross signing")

        val keyId = generateSequence {
            val alphabet = 'a'..'z'
            generateSequence { alphabet.random() }.take(24).joinToString("")
        }.first { keyStore.getSecretKeyEvent<SecretKeyEventContent>(key = it) == null }

        return BootstrapCrossSigning(
            recoveryKey = encodeRecoveryKey(recoveryKey),
            result = repository.setAccountData(secretKeyEventContent, userInfo.userId, keyId)
                .mapCatching { repository.setAccountData(DefaultSecretKeyEventContent(keyId), userInfo.userId) }
                .mapCatching {
                    val (masterSigningPrivateKey, masterSigningPublicKey) =
                        freeAfter(OlmPkSigning.create(null)) { it.privateKey to it.publicKey }
                    val masterSigningKey = signService.sign(
                        CrossSigningKeys(
                            userId = userInfo.userId,
                            usage = setOf(MasterKey),
                            keys = keysOf(Ed25519Key(masterSigningPublicKey, masterSigningPublicKey))
                        ),
                        signWith = SignWith.PrivateKey(
                            privateKey = masterSigningPrivateKey,
                            publicKey = masterSigningPublicKey
                        )
                    )
                    val encryptedMasterSigningKey = MasterKeyEventContent(
                        encryptSecret(recoveryKey, keyId, "m.cross_signing.master", masterSigningPrivateKey, json)
                    )
                    val (selfSigningPrivateKey, selfSigningPublicKey) =
                        freeAfter(OlmPkSigning.create(null)) { it.privateKey to it.publicKey }
                    val selfSigningKey = signService.sign(
                        CrossSigningKeys(
                            userId = userInfo.userId,
                            usage = setOf(SelfSigningKey),
                            keys = keysOf(Ed25519Key(selfSigningPublicKey, selfSigningPublicKey))
                        ),
                        signWith = SignWith.PrivateKey(
                            privateKey = masterSigningPrivateKey,
                            publicKey = masterSigningPublicKey
                        )
                    )
                    val encryptedSelfSigningKey = SelfSigningKeyEventContent(
                        encryptSecret(
                            recoveryKey,
                            keyId,
                            M_CROSS_SIGNING_SELF_SIGNING.id,
                            selfSigningPrivateKey,
                            json
                        )
                    )
                    val (userSigningPrivateKey, userSigningPublicKey) =
                        freeAfter(OlmPkSigning.create(null)) { it.privateKey to it.publicKey }
                    val userSigningKey = signService.sign(
                        CrossSigningKeys(
                            userId = userInfo.userId,
                            usage = setOf(UserSigningKey),
                            keys = keysOf(Ed25519Key(userSigningPublicKey, userSigningPublicKey))
                        ),
                        signWith = SignWith.PrivateKey(
                            privateKey = masterSigningPrivateKey,
                            publicKey = masterSigningPublicKey
                        )
                    )
                    val encryptedUserSigningKey = UserSigningKeyEventContent(
                        encryptSecret(
                            recoveryKey,
                            keyId,
                            M_CROSS_SIGNING_USER_SIGNING.id,
                            userSigningPrivateKey,
                            json
                        )
                    )
                    keyStore.updateSecrets {
                        mapOf(
                            M_CROSS_SIGNING_SELF_SIGNING to StoredSecret(
                                GlobalAccountDataEvent(encryptedSelfSigningKey),
                                selfSigningPrivateKey
                            ),
                            M_CROSS_SIGNING_USER_SIGNING to StoredSecret(
                                GlobalAccountDataEvent(encryptedUserSigningKey),
                                userSigningPrivateKey
                            ),
                        )
                    }
                    repository.setAccountData(encryptedMasterSigningKey, userInfo.userId)
                        .map { repository.setAccountData(encryptedUserSigningKey, userInfo.userId) }
                        .map { repository.setAccountData(encryptedSelfSigningKey, userInfo.userId) }
                        .map {
                            keyBackupService.bootstrapRoomKeyBackup(
                                recoveryKey,
                                keyId,
                                masterSigningPrivateKey,
                                masterSigningPublicKey
                            )
                        }
                        .map {
                            repository.setCrossSigningKeys(
                                masterKey = masterSigningKey,
                                selfSigningKey = selfSigningKey,
                                userSigningKey = userSigningKey
                            )
                        }
                }
                .mapCatching {
                    println("kostka_test, waiting for keys to be set")
                    println("kostka_test, bootstrapping keys SUCCESS")

                    keyStore.updateOutdatedKeys { oldOutdatedKeys -> oldOutdatedKeys + userInfo.userId }
                    println("kostka_test, updateOutdatedKeys finished")
                    val masterKey = keyStore.getCrossSigningKey(
                        userInfo.userId,
                        MasterKey
                    )?.value?.signed?.get<Ed25519Key>()
                    val ownDeviceKey = keyStore.getDeviceKey(
                        userInfo.userId.full,
                        userInfo.deviceId
                    )?.value?.get<Ed25519Key>()

                    keyTrustService.trustAndSignKeys(setOfNotNull(masterKey, ownDeviceKey), userInfo.userId)
                    println("wait for own device keys to be marked as cross signed and verified")
                    keyStore.getDeviceKey(userInfo.userId.full, userInfo.deviceId)
                    println("finished bootstrapping")
                }
        )
    }

    suspend fun bootstrapCrossSigning(): BootstrapCrossSigning {
        val recoveryKey = SecureRandom.nextBytes(32)
        val iv = SecureRandom.nextBytes(16)
        val secretKeyEventContent = AesHmacSha2Key(
            iv = iv.encodeBase64(),
            mac = createAesHmacSha2MacFromKey(recoveryKey, iv)
        )

        return bootstrapCrossSigning(recoveryKey, secretKeyEventContent)
    }
}