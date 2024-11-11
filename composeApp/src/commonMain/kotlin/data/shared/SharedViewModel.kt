package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.storage.Data
import koin.DeveloperUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedViewModel: ViewModel() {

    /** Singleton data manager to keep session-only data alive */
    protected val sharedDataManager: SharedDataManager = KoinPlatform.getKoin().get()

    /** lazily loaded repository for calling API */
    protected val sharedRepository: SharedRepository by KoinPlatform.getKoin().inject()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<Settings>()


    //======================================== developer tools ==========================================

    /** developer console size */
    val developerConsoleSize = sharedDataManager.developerConsoleSize.asStateFlow()

    /** log data associated with this apps' http calls */
    val httpLogData = sharedDataManager.httpLogData.asStateFlow()

    /** Current host override if there is any */
    val hostOverride = sharedDataManager.hostOverride.asStateFlow()


    //======================================== variables ==========================================

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
    val isToolbarExpanded = sharedDataManager.isToolbarExpanded.asStateFlow()


    //======================================== functions ==========================================


    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        sharedDataManager.isToolbarExpanded.value = expand
    }

    /** Changes the state of the developer console */
    fun changeDeveloperConsole(size: Float = developerConsoleSize.value) {
        sharedDataManager.developerConsoleSize.value = size
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

    /** Overrides current host */
    fun changeHost(host: String) {
        sharedDataManager.hostOverride.value = host
    }

    /** appends new or updates existing http log */
    @OptIn(ExperimentalUuidApi::class)
    fun appendHttpLog(call: DeveloperUtils.HttpCall?) {
        if(call == null) return
        sharedDataManager.httpLogData.value = DeveloperUtils.HttpLogData(
            id = Uuid.random().toString(),
            httpCalls = sharedDataManager.httpLogData.value.httpCalls.apply {
                find { it.id == call.id }?.update(call).ifNull {
                    add(call)
                }
            }
        )
    }
}

expect fun fromByteArrayToData(byteArray: ByteArray): Data