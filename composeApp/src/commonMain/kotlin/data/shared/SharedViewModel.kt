package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import com.russhwolf.settings.Settings
import data.io.app.LocalSettings
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.messaging.messaging
import dev.gitlive.firebase.storage.Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedViewModel: ViewModel() {

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager = KoinPlatform.getKoin().get()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<Settings>()

    /** Current configuration specific to this app */
    val localSettings = sharedDataManager.localSettings.asStateFlow()


    /** currently signed in firebase user */
    val firebaseUser = Firebase.auth.authStateChanged.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Firebase.auth.currentUser
    )

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = sharedDataManager.isToolbarExpanded

    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        sharedDataManager.isToolbarExpanded.value = expand
    }

    /** Logs out the currently signed in user */
    open fun logoutCurrentUser() {
        runBlocking {
            Firebase.auth.signOut()
        }
    }

    /** Initializes the application */
    fun initApp() {
        viewModelScope.launch {
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
                    fcmToken = fcmToken
                )
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (firebaseUser.value != null) {
                sharedDataManager.currentUser.update {
                    it?.copy(idToken = firebaseUser.value?.getIdToken(false))
                }
                Firebase.auth.idTokenChanged.collectLatest { firebaseUser ->
                    sharedDataManager.currentUser.update {
                        it?.copy(idToken = firebaseUser?.getIdToken(false))
                    }
                }
            }
        }
    }

    /** Updates with new token and sends this information to BE */
    fun updateFcmToken(newToken: String) {
        println("New FCM token: $newToken")
        sharedDataManager.localSettings.update {
            it?.copy(fcmToken = newToken)
        }
        viewModelScope.launch {
            //TODO send token to BE
        }
    }
}

expect fun fromByteArrayToData(byteArray: ByteArray): Data