package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import data.io.app.SettingsKeys
import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.storage.Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform

/** Viewmodel with shared behavior and injections for general purposes */
@OptIn(ExperimentalSettingsApi::class)
open class SharedViewModel: ViewModel() {

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()

    /** lazily loaded repository for calling API */
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<FlowSettings>()


    init {
        viewModelScope.launch(Dispatchers.IO) {
            delay(50)
            sharedDataManager.isToolbarExpanded.value = settings.getBooleanOrNull(SettingsKeys.KEY_TOOLBAR_EXPANDED) ?: true
        }
    }

    //======================================== public variables ==========================================

    /** Current configuration specific to this app */
    val localSettings = sharedDataManager.localSettings.asStateFlow()

    /** currently signed in firebase user */
    val firebaseUser = Firebase.auth.authStateChanged.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Firebase.auth.currentUser
    ).onEach { firebaseUser ->
        if(firebaseUser != null) {
            delay(500) // we have to delay the check to give time login flow to catch up
            if(sharedDataManager.currentUser.value == null) {
                firebaseUser.getIdToken(false)?.let { idToken ->
                    sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.copy(
                        idToken = idToken
                    ) ?: UserIO(idToken = idToken)

                    sharedDataManager.currentUser.value = sharedDataManager.currentUser.value?.update(
                        sharedRepository.authenticateUser(
                            localSettings = sharedDataManager.localSettings.value,
                            refreshToken = sharedDataManager.currentUser.value?.refreshToken,
                            expiresInMs = sharedDataManager.currentUser.value?.expiresInMs
                        )
                    )
                }
            }
        }
    }

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = sharedDataManager.isToolbarExpanded.asStateFlow()


    //======================================== functions ==========================================


    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        sharedDataManager.isToolbarExpanded.value = expand
        viewModelScope.launch {
            settings.putBoolean(SettingsKeys.KEY_TOOLBAR_EXPANDED, expand)
        }
    }

    /** Logs out the currently signed in user */
    open fun logoutCurrentUser() {
        runBlocking {
            Firebase.auth.signOut()
            sharedDataManager.currentUser.value = null
        }
    }
}

expect fun fromByteArrayToData(byteArray: ByteArray): Data