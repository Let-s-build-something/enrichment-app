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
import data.io.user.UserIO
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
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.mp.KoinPlatform

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedModel: ViewModel() {
    private val dataSyncService = KoinPlatform.getKoin().get<DataSyncService>()
    private val authService = KoinPlatform.getKoin().get<AuthService>()

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager by KoinPlatform.getKoin().inject()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<AppSettings>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            delay(50)
            sharedDataManager.isToolbarExpanded.value = settings.getBooleanOrNull(SettingsKeys.KEY_TOOLBAR_EXPANDED) != false
        }

        viewModelScope.launch {
            delay(200)
            sharedDataManager.matrixClient.combine(currentUser) { client, user ->
                client to user
            }.collectLatest { client ->
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
                    authService.setupAutoLogin(forceRefresh = true)
                    updateClientSettings()

                    currentUser.value?.accessToken != null && currentUser.value?.matrixHomeserver != null
                }catch (e: Exception) {
                    println("kostka_test, initUser exception: ${e.message}")
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

    fun consumePing(identifier: String) {
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
            "${SettingsKeys.KEY_NETWORK_COLORS}_${currentUser.value?.matrixUserId}"
        )?.split(",")
            ?: NetworkProximityCategory.entries.map { it.color.asSimpleString() }

        val update = LocalSettings(
            theme = theme,
            fcmToken = fcmToken,
            clientStatus = clientStatus,
            networkColors = networkColors
        )
        sharedDataManager.localSettings.value = sharedDataManager.localSettings.value?.update(update) ?: update
    }

    /** Logs out the currently signed in user */
    open fun logoutCurrentUser() {
        runBlocking {
            authService.clear()
            secureSettings.clear()
            Firebase.auth.signOut()
            sharedDataManager.matrixClient.value?.logout()
            sharedDataManager.currentUser.value = null
            sharedDataManager.localSettings.value = null
            sharedDataManager.matrixClient.value = null
            sharedDataManager.olmAccount = null
            dataSyncService.stop()
            unloadKoinModules(commonModule)
            loadKoinModules(commonModule)
        }
    }
}

expect fun fromByteArrayToData(byteArray: ByteArray): Data