package data.shared.crypto

import data.shared.crypto.model.StoredSecret
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.folivo.trixnity.clientserverapi.model.keys.SetRoomKeyBackupVersionRequest
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.GlobalAccountDataEvent
import net.folivo.trixnity.core.model.events.m.MegolmBackupV1EventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.RoomKeyBackupAuthData
import net.folivo.trixnity.core.model.keys.keysOf
import net.folivo.trixnity.crypto.SecretType
import net.folivo.trixnity.crypto.key.encryptSecret
import net.folivo.trixnity.crypto.sign.SignService
import net.folivo.trixnity.crypto.sign.SignWith
import net.folivo.trixnity.olm.OlmPkDecryption
import net.folivo.trixnity.olm.freeAfter

class KeyBackupService(
    userInfo: UserInfo,
    private val json: Json,
    private val repository: EncryptionServiceRepository,
    private val signService: SignService,
    private val keyStore: OlmCryptoStore
) {
    private val ownUserId = userInfo.userId

    suspend fun bootstrapRoomKeyBackup(
        key: ByteArray,
        keyId: String,
        masterSigningPrivateKey: String,
        masterSigningPublicKey: String,
    ): Result<Unit> {
        val (keyBackupPrivateKey, keyBackupPublicKey) = freeAfter(OlmPkDecryption.create(null)) { it.privateKey to it.publicKey }
        return repository.setRoomKeysVersion(
            SetRoomKeyBackupVersionRequest.V1(
                authData = with(
                    RoomKeyBackupAuthData.RoomKeyBackupV1AuthData(Key.Curve25519Key(null, keyBackupPublicKey))
                ) {
                    val ownDeviceSignature = signService.signatures(this)[ownUserId]
                        ?.firstOrNull()
                    val ownUsersSignature =
                        signService.signatures(
                            this,
                            SignWith.PrivateKey(masterSigningPrivateKey, masterSigningPublicKey)
                        )[ownUserId]
                            ?.firstOrNull()
                    requireNotNull(ownUsersSignature)
                    requireNotNull(ownDeviceSignature)
                    copy(signatures = signatures + (ownUserId to keysOf(ownDeviceSignature, ownUsersSignature)))
                },
                version = null // create new version
            )
        ).map {
            val encryptedBackupKey = MegolmBackupV1EventContent(
                encryptSecret(key, keyId, SecretType.M_MEGOLM_BACKUP_V1.id, keyBackupPrivateKey, json)
            )
            keyStore.updateSecrets {
                it + (SecretType.M_MEGOLM_BACKUP_V1 to StoredSecret(
                    GlobalAccountDataEvent(encryptedBackupKey),
                    keyBackupPrivateKey
                ))
            }
            repository.setAccountData(encryptedBackupKey, ownUserId)
        }
    }
}

suspend inline fun <reified T> SignService.signatures(
    unsignedObject: T,
    signWith: SignWith = SignWith.DeviceKey
): Signatures<UserId> =
    signatures(unsignedObject, serializer(), signWith)
