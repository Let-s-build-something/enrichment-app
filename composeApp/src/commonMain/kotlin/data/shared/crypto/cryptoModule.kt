package data.shared.crypto

import data.shared.SharedDataManager
import data.shared.sync.ClientEventEmitter
import data.shared.sync.SyncResponseEmitter
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.ClientEventEmitter.Priority
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.crypto.olm.OlmDecrypterImpl
import net.folivo.trixnity.crypto.olm.OlmEncryptionService
import net.folivo.trixnity.crypto.olm.OlmEncryptionServiceImpl
import net.folivo.trixnity.crypto.olm.OlmEventHandler
import net.folivo.trixnity.crypto.olm.OlmEventHandlerRequestHandler
import net.folivo.trixnity.crypto.olm.OlmKeysChange
import net.folivo.trixnity.crypto.olm.OlmKeysChangeEmitter
import net.folivo.trixnity.crypto.sign.SignServiceImpl
import net.folivo.trixnity.crypto.sign.SignServiceStore
import net.folivo.trixnity.olm.OlmAccount
import net.folivo.trixnity.olm.freeAfter
import org.koin.core.module.Module
import org.koin.dsl.module
import ui.login.AUGMY_HOME_SERVER

internal suspend fun cryptoModule(
    sharedDataManager: SharedDataManager
): Module {
    val pickleKey = sharedDataManager.localSettings.value?.pickleKey
    val deviceId = sharedDataManager.localSettings.value?.deviceId
    val userId = sharedDataManager.currentUser.value?.matrixUserId

    return if (pickleKey != null && deviceId != null && userId != null) {
        val olmStore = OlmCryptoStore(sharedDataManager)

        val (signingKey, identityKey) = freeAfter(
            sharedDataManager.olmAccount
                ?: olmStore.getOlmAccount().takeIf { it.isNotBlank() }?.let { pickle ->
                    OlmAccount.unpickle(key = pickleKey, pickle = pickle)
                } ?: OlmAccount.create().also { olmAccount ->
                    olmStore.updateOlmAccount {
                        olmAccount.pickle(key = pickleKey)
                    }
                }
        ) {
            sharedDataManager.olmAccount = it
            Key.Ed25519Key(deviceId, it.identityKeys.ed25519) to
                    Key.Curve25519Key(deviceId, it.identityKeys.curve25519)
        }


        val userInfo = UserInfo(
            userId = UserId(full = userId),
            deviceId = deviceId,
            signingPublicKey = signingKey,
            identityPublicKey = identityKey
        )
        val requestHandler = EncryptionServiceRepository(
            homeserver = {
                sharedDataManager.currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER
            },
            userInfo = userInfo,
            keyStore = olmStore
        )
        val signServiceStore = object: SignServiceStore {
            override suspend fun getOlmAccount(): String = olmStore.getOlmAccount()
            override suspend fun getOlmPickleKey(): String = olmStore.getOlmPickleKey()
        }
        val clientEventEmitter = ClientEventEmitter()
        val syncResponseEmitter = SyncResponseEmitter()

        module {
            single<OlmCryptoStore> { olmStore }
            single<EncryptionServiceRepository> { requestHandler }
            single<SignServiceStore> { signServiceStore }
            single<UserInfo> { userInfo }
            single<ClientEventEmitter> { clientEventEmitter }
            single<SyncResponseEmitter> { syncResponseEmitter }

            single<OlmEncryptionService> {
                OlmEncryptionServiceImpl(
                    json = get<Json>(),
                    clock = Clock.System,
                    signService = SignServiceImpl(
                        json = get<Json>(),
                        userInfo = userInfo,
                        store = signServiceStore
                    ),
                    requests = requestHandler,
                    store = olmStore,
                    userInfo = userInfo
                )
            }
            single<OlmEventHandler> {
                OlmEventHandler(
                    userInfo = userInfo,
                    eventEmitter = clientEventEmitter,
                    olmKeysChangeEmitter = object: OlmKeysChangeEmitter {
                        override fun subscribeOneTimeKeysCount(subscriber: suspend (OlmKeysChange) -> Unit): () -> Unit {
                            return syncResponseEmitter.subscribe(Priority.ONE_TIME_KEYS) {
                                subscriber(OlmKeysChange(it.syncResponse.oneTimeKeysCount, it.syncResponse.unusedFallbackKeyTypes))
                            }
                        }
                    },
                    decrypter = OlmDecrypterImpl(
                        olmEncryptionService = get<OlmEncryptionService>(),
                    ),
                    signService = SignServiceImpl(
                        store = signServiceStore,
                        json = get<Json>(),
                        userInfo = userInfo
                    ),
                    requestHandler = object: OlmEventHandlerRequestHandler {
                        override suspend fun setOneTimeKeys(
                            oneTimeKeys: Keys?,
                            fallbackKeys: Keys?
                        ): Result<Unit> {
                            return requestHandler.setOneTimeKeys(
                                deviceKeys = null,
                                oneTimeKeys = oneTimeKeys,
                                fallbackKeys = fallbackKeys
                            ).map {  }
                        }
                    },
                    store = olmStore,
                    clock = Clock.System
                )
            }
        }
    }else module {  }
}