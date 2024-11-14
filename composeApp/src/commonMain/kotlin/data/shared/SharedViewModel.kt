package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import data.io.app.SettingsKeys
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.storage.Data
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedViewModel: ViewModel() {

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager = KoinPlatform.getKoin().get()

    /** lazily loaded repository for calling API */
    protected val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<Settings>()

    //======================================== public variables ==========================================

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

    private val _isToolbarExpanded = MutableStateFlow(
        settings.getBooleanOrNull(SettingsKeys.KEY_TOOLBAR_EXPANDED) ?: true
    )

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = _isToolbarExpanded.asStateFlow()


    //======================================== functions ==========================================


    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        _isToolbarExpanded.value = expand
        settings.putBoolean(SettingsKeys.KEY_TOOLBAR_EXPANDED, expand)
    }

    /** Logs out the currently signed in user */
    open fun logoutCurrentUser() {
        runBlocking {
            Firebase.auth.signOut()
            sharedDataManager.currentUser.value = null
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