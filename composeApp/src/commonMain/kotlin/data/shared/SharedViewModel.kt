package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import base.utils.NetworkConnectivity
import base.utils.NetworkSpeed
import data.io.app.SettingsKeys
import data.io.base.AppPing
import data.io.user.UserIO
import data.shared.auth.AuthService
import data.shared.sync.DataSyncService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.storage.Data
import koin.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedViewModel: ViewModel() {
    private val dataSyncService = KoinPlatform.getKoin().get<DataSyncService>()
    private val authService = KoinPlatform.getKoin().get<AuthService>()

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()

    /** lazily loaded repository for calling API */
    private val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<AppSettings>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            delay(50)
            sharedDataManager.isToolbarExpanded.value = settings.getBooleanOrNull(SettingsKeys.KEY_TOOLBAR_EXPANDED) ?: true
        }

        viewModelScope.launch {
            delay(1000)
            currentUser.collectLatest { user ->
                if(user?.accessToken != null && user.matrixHomeserver != null) {
                    dataSyncService.sync(homeserver = user.matrixHomeserver)
                }else dataSyncService.stop()
            }
        }
    }

    //======================================== public variables ==========================================

    val awaitingAutologin: Boolean
        get() = authService.awaitingAutologin


    /** Current configuration specific to this app */
    val localSettings = sharedDataManager.localSettings.asStateFlow()

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** Acts as a sort of a in-app push notification, notifying of changes */
    val pingStream = sharedDataManager.pingStream.asSharedFlow()

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
                    authService.setupAutoLogin()
                }
            }
        }
    }

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = sharedDataManager.isToolbarExpanded.asStateFlow()

    /** Most recent measure of speed and network connectivity */
    val networkConnectivity = sharedDataManager.networkConnectivity.asStateFlow()


    //======================================== functions ==========================================

    fun updateNetworkConnectivity(
        isNetworkAvailable: Boolean? = null,
        networkSpeed: NetworkSpeed? = null
    ) {
        sharedDataManager.networkConnectivity.value = sharedDataManager.networkConnectivity.value?.copy(
            isNetworkAvailable = isNetworkAvailable ?: sharedDataManager.networkConnectivity.value?.isNetworkAvailable,
            speed = networkSpeed ?: sharedDataManager.networkConnectivity.value?.speed
        ) ?: NetworkConnectivity(
            isNetworkAvailable = isNetworkAvailable,
            speed = networkSpeed
        )
    }

    fun setOfflineMode(offlineMode: Boolean) {
        sharedDataManager.networkConnectivity.value = sharedDataManager.networkConnectivity.value?.copy(
            offlineMode = offlineMode
        )
    }

    fun consumePing(ping: AppPing) {
        sharedDataManager.pingStream.value = sharedDataManager.pingStream.value.minus(ping)
    }

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
            sharedDataManager.localSettings.value = null
            authService.clear()
            dataSyncService.stop()
        }
    }
}

expect fun fromByteArrayToData(byteArray: ByteArray): Data