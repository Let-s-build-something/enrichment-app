package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import base.utils.NetworkConnectivity
import base.utils.NetworkSpeed
import base.utils.asSimpleString
import data.NetworkProximityCategory
import data.io.app.ClientStatus
import data.io.app.LocalSettings
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.io.base.AppPing
import data.shared.auth.AuthService
import data.shared.sync.DataSyncService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.messaging.messaging
import dev.gitlive.firebase.storage.Data
import koin.AppSettings
import koin.commonModule
import koin.secureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.mp.KoinPlatform
import ui.login.AUGMY_HOME_SERVER

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedModel: ViewModel() {
    protected val dataSyncService = KoinPlatform.getKoin().get<DataSyncService>()
    protected val authService = KoinPlatform.getKoin().get<AuthService>()

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()

    /** persistent settings saved locally to a device */
    protected val settings: AppSettings by KoinPlatform.getKoin().inject()

    //======================================== public variables ==========================================

    val matrixUserId: String?
        get() = currentUser.value?.matrixUserId ?: authService.storedUserId()

    val homeserver: String
        get() = currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER

    val awaitingAutologin: Boolean
        get() = authService.awaitingAutologin


    /** Current configuration specific to this app */
    val localSettings = sharedDataManager.localSettings.asStateFlow()

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** Acts as a sort of a in-app push notification, notifying of changes */
    val pingStream = sharedDataManager.pingStream.asStateFlow()

    /** currently signed in firebase user */
    val firebaseUser = Firebase.auth.authStateChanged.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Firebase.auth.currentUser
    ).onEach { firebaseUser ->
        if(firebaseUser != null) {
            delay(500) // we have to delay the check to give time login flow to catch up
            initUser()
        }
    }

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = sharedDataManager.isToolbarExpanded.asStateFlow()

    /** Most recent measure of speed and network connectivity */
    val networkConnectivity = sharedDataManager.networkConnectivity.asStateFlow()


    //======================================== functions ==========================================

    /** Initializes the user and returns whether successful */
    private suspend fun initUser(): Boolean {
        return Firebase.auth.currentUser?.let {
            if(sharedDataManager.currentUser.value?.idToken == null) {
                try {
                    authService.setupAutoLogin(forceRefresh = false)
                    updateClientSettings()

                    currentUser.value?.accessToken != null && currentUser.value?.matrixHomeserver != null
                }catch (e: Exception) {
                    authService.stop()
                    authService.setupAutoLogin()
                    e.printStackTrace()
                    currentUser.value?.accessToken != null && currentUser.value?.matrixHomeserver != null
                }
            }else false
        } ?: false
    }

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

    fun consumePing(ping: AppPing) {
        sharedDataManager.pingStream.value = sharedDataManager.pingStream.value.minus(ping)
    }

    suspend fun consumePing(identifier: String) = withContext(Dispatchers.Default) {
        sharedDataManager.pingStream.update {
            it.filter { ping ->
                !ping.identifiers.contains(identifier)
            }.toSet()
        }
    }

    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        sharedDataManager.isToolbarExpanded.value = expand
        viewModelScope.launch {
            settings.putBoolean(SettingsKeys.KEY_TOOLBAR_EXPANDED, expand)
        }
    }

    suspend fun updateClientSettings() {
        val fcmToken = settings.getStringOrNull(SettingsKeys.KEY_FCM) ?: try {
            Firebase.messaging.getToken()
        }catch (_: NotImplementedError) { null }?.apply {
            settings.putString(SettingsKeys.KEY_FCM, this)
        }
        val theme = ThemeChoice.entries.find {
            it.name == settings.getStringOrNull(SettingsKeys.KEY_THEME)
        } ?: ThemeChoice.DARK
        val clientStatus = ClientStatus.entries.find {
            it.name == settings.getStringOrNull(SettingsKeys.KEY_CLIENT_STATUS)
        } ?: ClientStatus.NEW
        val networkColors = settings.getStringOrNull(
            "${SettingsKeys.KEY_NETWORK_COLORS}_$matrixUserId"
        )?.split(",")
            ?: NetworkProximityCategory.entries.map { it.color.asSimpleString() }
        // Jvm_3b158f7e3e20467681c9ac573ffb9fd6

        val update = LocalSettings(
            theme = theme,
            fcmToken = fcmToken,
            clientStatus = clientStatus,
            networkColors = networkColors,
            deviceId = authService.getDeviceId()
        )
        sharedDataManager.localSettings.value = sharedDataManager.localSettings.value?.update(update) ?: update
    }

    /** Logs out the currently signed in user */
    open suspend fun logoutCurrentUser() {
        dataSyncService.stop()
        authService.clear()
        secureSettings.clear()
        Firebase.auth.signOut()
        sharedDataManager.matrixClient.value?.logout()
        sharedDataManager.currentUser.value = null
        sharedDataManager.localSettings.value = null
        sharedDataManager.matrixClient.value = null
        sharedDataManager.pingStream.value = setOf()
        unloadKoinModules(commonModule)
        loadKoinModules(commonModule)
        updateClientSettings()
    }
}

expect fun fromByteArrayToData(byteArray: ByteArray): Data
