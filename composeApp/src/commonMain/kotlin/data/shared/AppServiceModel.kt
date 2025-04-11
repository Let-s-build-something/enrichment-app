package data.shared

import androidx.lifecycle.viewModelScope
import base.utils.deeplinkHost
import data.io.app.ClientStatus
import data.io.app.SettingsKeys
import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import korlibs.io.net.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val appServiceModule = module {
    single<AppServiceDataManager> { AppServiceDataManager() }
    factory { AppServiceRepository(get()) }
    factory { AppServiceModel(get(), get()) }
    viewModelOf(::AppServiceModel)
}

class AppServiceDataManager {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = MutableSharedFlow<String>()
}

/** Shared viewmodel for services specific to the runtime of the application */
class AppServiceModel(
    private val repository: AppServiceRepository,
    private val dataManager: AppServiceDataManager
): SharedModel() {

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

        viewModelScope.launch(Dispatchers.IO) {
            delay(50)
            sharedDataManager.isToolbarExpanded.value = settings.getBooleanOrNull(SettingsKeys.KEY_TOOLBAR_EXPANDED) != false
        }

        viewModelScope.launch {
            delay(200)
            sharedDataManager.matrixClient.combine(currentUser) { client, user ->
                client to user
            }.collectLatest { client ->
                println("kostka_test, awaiting sync: client: ${client.first}, user: ${client.second}")
                if(client.first != null && client.second?.isFullyValid == true) {
                    client.second?.matrixHomeserver?.let { homeserver ->
                        dataSyncService.sync(homeserver = homeserver)
                    }
                }
            }
        }
        viewModelScope.launch {
            delay(200)
            // TODO there should be a variant for unsigned invalid clients as well, probably only a ping of sorts
            networkConnectivity.collectLatest {
                sharedDataManager.currentUser.value?.matrixHomeserver?.let { homeserver ->
                    while(it?.isNetworkAvailable == false) {
                        dataSyncService.stop()
                        dataSyncService.sync(homeserver = homeserver, delay = 2000)
                        delay(3000)
                    }
                }
            }
        }

        // update idToken whenever it changes
        Firebase.auth.idTokenChanged.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            Firebase.auth.currentUser
        ).onEach { firebaseUser ->
            if(firebaseUser != null) {
                val update = UserIO(idToken = firebaseUser.getIdToken(false))
                sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(update) ?: update
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
            updateClientSettings()
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
                uri.replace("""^\/""".toRegex(), "")
                    .replace("""\/$""".toRegex(), "")
                    .replace(deeplinkHost, "")
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