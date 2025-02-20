package data.shared

import androidx.lifecycle.viewModelScope
import base.utils.asSimpleString
import data.NetworkProximityCategory
import data.io.app.ClientStatus
import data.io.app.LocalSettings
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.shared.crypto.OlmCryptoStore
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import korlibs.io.net.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.model.keys.ClaimKeys
import net.folivo.trixnity.core.UserInfo
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ToDeviceEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm
import net.folivo.trixnity.crypto.olm.OlmEncryptionServiceImpl
import net.folivo.trixnity.crypto.olm.OlmEncryptionServiceRequestHandler
import net.folivo.trixnity.crypto.olm.OlmStore
import net.folivo.trixnity.crypto.sign.SignServiceImpl
import net.folivo.trixnity.crypto.sign.SignServiceStore
import net.folivo.trixnity.olm.OlmAccount
import net.folivo.trixnity.olm.freeAfter
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

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

        module {
            single { olmStore }
            single {
                object: OlmEncryptionServiceRequestHandler {
                    override suspend fun claimKeys(oneTimeKeys: Map<UserId, Map<String, KeyAlgorithm>>): Result<ClaimKeys.Response> {
                        TODO("Not yet implemented")
                    }
                    override suspend fun sendToDevice(events: Map<UserId, Map<String, ToDeviceEventContent>>): Result<Unit> {
                        TODO("Not yet implemented")
                    }
                }
            }
            single {
                object: SignServiceStore {
                    override suspend fun getOlmAccount(): String {
                        TODO("Not yet implemented")
                    }
                    override suspend fun getOlmPickleKey(): String {
                        TODO("Not yet implemented")
                    }
                }
            }
            single {
                UserInfo(
                    userId = UserId(full = userId),
                    deviceId = deviceId,
                    signingPublicKey = signingKey,
                    identityPublicKey = identityKey
                )
            }
            single {
                val userInfo = getOrNull<UserInfo>()
                if(userInfo != null) {
                    OlmEncryptionServiceImpl(
                        json = get<Json>(),
                        clock = Clock.System,
                        signService = SignServiceImpl(
                            json = get<Json>(),
                            userInfo = get<UserInfo>(),
                            store = get<SignServiceStore>()
                        ),
                        requests = get<OlmEncryptionServiceRequestHandler>(),
                        store = get<OlmStore>(),
                        userInfo = get<UserInfo>()
                    )
                }
            }
        }
    }else module {  }
}

internal val appServiceModule = module {
    single<AppServiceDataManager> { AppServiceDataManager() }
    factory { AppServiceRepository(get()) }
    factory { AppServiceViewModel(get(), get()) }
    viewModelOf(::AppServiceViewModel)
}

class AppServiceDataManager {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = MutableSharedFlow<String>()
}

/** Shared viewmodel for services specific to the runtime of the application */
class AppServiceViewModel(
    private val repository: AppServiceRepository,
    private val dataManager: AppServiceDataManager
): SharedViewModel() {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = dataManager.newDeeplink.asSharedFlow()

    /** current client status */
    val clientStatus = MutableStateFlow<ClientStatus?>(null)

    /** Whether leave dialog should be shown */
    var showLeaveDialog: Boolean = true

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ClientStatus.entries.find {
                    it.name == settings.getStringOrNull(SettingsKeys.KEY_CLIENT_STATUS)
                }.let { status ->
                    clientStatus.emit(status ?: ClientStatus.NEW)
                }
            }
        }
    }

    /** Initializes the application */
    fun initApp() {
        CoroutineScope(Dispatchers.IO).launch {
            showLeaveDialog = settings.getBooleanOrNull(SettingsKeys.KEY_SHOW_LEAVE_DIALOG) ?: true

            // add missing mimetypes
            MimeType.register(
                MimeType("audio/mpeg", listOf("mp3")),
                MimeType("audio/ogg", listOf("ogg")),
                MimeType("audio/wav", listOf("wav")),
                MimeType("audio/x-aac", listOf("aac")),
                MimeType("audio/x-flac", listOf("flac")),
                MimeType("video/mp4", listOf("mp4")),
                MimeType("video/x-msvideo", listOf("avi")),
                MimeType("video/x-matroska", listOf("mkv")),
                MimeType("video/webm", listOf("webm")),
                MimeType("application/pdf", listOf("pdf")),
                MimeType("application/vnd.ms-powerpoint", listOf("ppt", "pps")),
                MimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation", listOf("pptx")),
                MimeType("application/vnd.openxmlformats-officedocument.presentationml.slideshow", listOf("ppsx")),
                MimeType("application/zip", listOf("zip")),
                MimeType("application/x-rar-compressed", listOf("rar")),
                MimeType("application/x-7z-compressed", listOf("7z")),
                MimeType("application/gzip", listOf("gz")),
                MimeType("image/svg+xml", listOf("svg")),
                MimeType("image/webp", listOf("webp"))
            )

            if (sharedDataManager.localSettings.value == null) {
                val defaultFcm = settings.getStringOrNull(SettingsKeys.KEY_FCM)

                val fcmToken = if(defaultFcm == null) {
                    val newFcm = try {
                        Firebase.messaging.getToken()
                    }catch (_: NotImplementedError) { null }?.apply {
                        settings.putString(SettingsKeys.KEY_FCM, this)
                    }
                    newFcm
                }else defaultFcm

                val update = LocalSettings(
                    theme = ThemeChoice.entries.find {
                        it.name == settings.getStringOrNull(SettingsKeys.KEY_THEME)
                    } ?: ThemeChoice.SYSTEM,
                    fcmToken = fcmToken,
                    clientStatus = ClientStatus.entries.find {
                        it.name == settings.getStringOrNull(SettingsKeys.KEY_CLIENT_STATUS)
                    } ?: ClientStatus.NEW,
                    networkColors = settings.getStringOrNull(SettingsKeys.KEY_NETWORK_COLORS)?.split(",")
                        ?: NetworkProximityCategory.entries.map { it.color.asSimpleString() }
                )
                sharedDataManager.localSettings.value = sharedDataManager.localSettings.value?.update(update) ?: update
            }
        }
    }

    /** Save settings for leave dialog */
    fun saveDialogSetting(showAgain: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            settings.putBoolean(SettingsKeys.KEY_SHOW_LEAVE_DIALOG, showAgain)
        }
    }

    /** Emits a new deep link for handling */
    fun emitDeepLink(uri: String?) {
        if(uri == null) return
        viewModelScope.launch {
            dataManager.newDeeplink.emit(
                uri
                    .replace("""^\/""".toRegex(), "")
                    .replace("""\/$""".toRegex(), "")
                    .replace("augmy://", "")
            )
        }
    }

    /** Updates with new token and sends this information to BE */
    fun updateFcmToken(newToken: String) {
        viewModelScope.launch {
            repository.updateFCMToken(
                prevFcmToken = sharedDataManager.localSettings.value?.fcmToken,
                publicId = sharedDataManager.currentUser.value?.publicId,
                newToken = newToken
            )
        }
        sharedDataManager.localSettings.update {
            it?.copy(fcmToken = newToken)
        }
    }
}