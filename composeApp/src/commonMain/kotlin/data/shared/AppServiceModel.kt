package data.shared

import androidx.core.uri.UriUtils
import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.utils.DateUtils
import base.utils.deeplinkHost
import data.io.app.ClientStatus
import data.io.app.SettingsKeys
import data.io.app.SettingsKeys.KEY_REFEREE_USER_ID
import data.io.app.SettingsKeys.KEY_REFERRER_FINISHED
import data.io.base.AppPingType
import korlibs.io.net.MimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.login.strippedUsernameRegex
import utils.SharedLogger

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

    val showDevelopmentConsole = MutableStateFlow(false)

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

        viewModelScope.launch {
            showDevelopmentConsole.value = repository.checkIsDeveloper(matrixUserId).also {
                if (it) SharedLogger.init(isDevelopment = true)
            }

            sharedDataManager.pingStream.collect { pings ->
                if (!showDevelopmentConsole.value && pings.any { it.type == AppPingType.ConversationDashboard }) {
                    showDevelopmentConsole.value = repository.checkIsDeveloper(matrixUserId).also {
                        if (it) SharedLogger.init(isDevelopment = true)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            delay(50)
            sharedDataManager.isToolbarExpanded.value = settings.getBooleanOrNull(SettingsKeys.KEY_TOOLBAR_EXPANDED) != false
        }

        viewModelScope.launch {
            delay(200)
            var lastConnectivity = networkConnectivity.value?.isNetworkAvailable
            networkConnectivity.collectLatest {
                // retry for signed in users, we don't really care about unsigned
                sharedDataManager.currentUser.value?.matrixHomeserver?.let { homeserver ->
                    if(lastConnectivity == false && it?.isNetworkAvailable == true) {
                        syncService.stop()
                        syncService.sync(homeserver = homeserver, delay = 2000)
                    }
                    lastConnectivity = it?.isNetworkAvailable
                }
            }
        }
    }

    /** Initializes the application */
    fun initApp() {
        SharedLogger.init()
        SharedLogger.logger.debug { "App initialized" }

        CoroutineScope(Dispatchers.IO).launch {
            if (settings.getStringOrNull(SettingsKeys.KEY_DOWNLOAD_TIME) == null) {
                settings.putString(SettingsKeys.KEY_DOWNLOAD_TIME, DateUtils.localNow.toString())
            }
            showLeaveDialog = settings.getBooleanOrNull(SettingsKeys.KEY_SHOW_LEAVE_DIALOG) != false

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
            initUser()
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
            val strippedUri = uri.replace("""^\/""".toRegex(), "")
                .replace("""\/$""".toRegex(), "")
                .replace(deeplinkHost, "")

            val uriObject = UriUtils.parse(strippedUri)
            if (uriObject.getPathSegments().firstOrNull() == "referral"
                && settings.getBooleanOrNull(KEY_REFERRER_FINISHED) != true
            ) {
                uriObject.getQueryParameters("user").firstOrNull()?.let { query ->
                    if (strippedUsernameRegex.matches(query)) {
                        settings.putString(KEY_REFEREE_USER_ID, query)
                        settings.putBoolean(KEY_REFERRER_FINISHED, true)
                    }
                }
            }else {
                dataManager.newDeeplink.emit(strippedUri)
            }
        }
    }

    /** Updates with new token and sends this information to BE */
    fun updateFcmToken(newToken: String) {
        viewModelScope.launch {
            repository.updateFCMToken(
                prevFcmToken = sharedDataManager.localSettings.value?.fcmToken,
                userId = sharedDataManager.currentUser.value?.userId,
                newToken = newToken
            )
        }
        sharedDataManager.localSettings.update {
            it?.copy(fcmToken = newToken)
        }
    }
}