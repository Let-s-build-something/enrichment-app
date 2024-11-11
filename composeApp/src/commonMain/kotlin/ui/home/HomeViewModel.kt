package ui.home

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import components.pull_refresh.RefreshableViewModel
import data.io.app.ClientStatus
import data.io.app.LocalSettings
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.io.user.UserIO
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    factory { HomeViewModel() }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel: SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    /** Newly emitted deep link which should be handled */
    val newDeeplink = sharedDataManager.newDeeplink.asSharedFlow()

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

    /** Emits a new deep link for handling */
    fun emitDeepLink(uri: String?) {
        println("emitDeepLink, path: $uri")

        if(uri == null) return
        viewModelScope.launch {
            sharedDataManager.newDeeplink.emit(
                uri
                    .replace("""^\/""".toRegex(), "")
                    .replace("""\/$""".toRegex(), "")
                    .replace("augmy://", "")
            )
        }
    }
}