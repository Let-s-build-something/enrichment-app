package data.shared

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import data.io.app.ClientStatus
import data.io.app.LocalSettings
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val appServiceModule = module {
    single<AppServiceDataManager> { AppServiceDataManager() }
    factory { AppServiceViewModel(get()) }
    viewModelOf(::AppServiceViewModel)
}

class AppServiceDataManager {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = MutableSharedFlow<String>()
}

/** Shared viewmodel for services specific to the runtime of the application */
class AppServiceViewModel(private val dataManager: AppServiceDataManager): SharedViewModel() {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = dataManager.newDeeplink.asSharedFlow()

    /** Whether leave dialog should be shown */
    val showLeaveDialog: Boolean
        get() = settings.getBooleanOrNull(SettingsKeys.SHOW_LEAVE_DIALOG) ?: true

    /** Initializes the application */
    fun initApp() {
        CoroutineScope(Dispatchers.IO).launch {
            if (sharedDataManager.localSettings.value == null) {
                val defaultFcm = settings.getStringOrNull(SettingsKeys.KEY_FCM)

                val fcmToken = if(defaultFcm == null) {
                    val newFcm = try {
                        // gotta wait for APNS to be ready before FCM request
                        if(currentPlatform == PlatformType.Native) delay(500)
                        Firebase.messaging.getToken()
                    }catch (e: NotImplementedError) { null }?.apply {
                        settings.putString(SettingsKeys.KEY_FCM, this)
                    }
                    newFcm
                }else defaultFcm

                sharedDataManager.localSettings.value = LocalSettings(
                    theme = ThemeChoice.entries.find {
                        it.name == settings.getStringOrNull(SettingsKeys.KEY_THEME)
                    } ?: ThemeChoice.SYSTEM,
                    fcmToken = fcmToken,
                    clientStatus = ClientStatus.entries.find {
                        it.name == settings.getStringOrNull(SettingsKeys.KEY_CLIENT_STATUS)
                    } ?: ClientStatus.NEW
                )
            }

            if (firebaseUser.value != null) {
                if(sharedDataManager.currentUser.value == null) {
                    firebaseUser.value?.getIdToken(false)?.let { idToken ->
                        sharedDataManager.currentUser.value = UserIO(idToken = idToken)
                        sharedDataManager.currentUser.value = sharedRepository.authenticateUser(
                            localSettings = sharedDataManager.localSettings.value
                        )?.copy(
                            idToken = idToken
                        )
                    }
                }
                Firebase.auth.idTokenChanged.collectLatest { firebaseUser ->
                    sharedDataManager.currentUser.update {
                        it?.copy(idToken = firebaseUser?.getIdToken(false))
                    }
                }
            }
        }
    }

    /** Save settings for leave dialog */
    fun saveDialogSetting(showAgain: Boolean) {
        settings.putBoolean(SettingsKeys.SHOW_LEAVE_DIALOG, showAgain)
    }

    /** Emits a new deep link for handling */
    fun emitDeepLink(uri: String?) {
        println("emitDeepLink, path: $uri")

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
}