package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import base.PlatformFirebase
import com.russhwolf.settings.Settings
import data.io.CloudUser
import data.io.CloudUserHelper
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.koin.mp.KoinPlatform
import ui.home.HomeViewModel.Companion.SETTINGS_KEY_AUTO_EMAIL
import ui.home.HomeViewModel.Companion.SETTINGS_KEY_AUTO_PASSWORD
import ui.login.UserOperationService

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedViewModel: ViewModel() {

    /** Singleton data manager to keep session-only data alive */
    protected val dataManager: SharedDataManager = KoinPlatform.getKoin().get()

    /** service provider for user related operations */
    protected val userOperationService = KoinPlatform.getKoin().get<UserOperationService>()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<Settings>()



    companion object {
        /** Epoch seconds before expiration time, that we want to request a new refresh token */
        private const val EXPIRE_BOUNDARY = 20L
    }

    /** currently signed in user */
    val currentUser = (PlatformFirebase?.auth?.authStateChanged
        ?.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PlatformFirebase?.auth?.currentUser
        ) ?: flowOf(null))
        .combine(dataManager.overrideUser) { firebaseUser, cloudUser ->
            cloudUser ?: CloudUserHelper.fromFirebaseUser(firebaseUser)
        }
        .onEach {
            if(dataManager.overrideUser.value != null) {
                scheduleExpirationDate()
            }
        }

    /** Overrides current user object */
    fun overrideCurrentUser(user: CloudUser?) {
        dataManager.overrideUser.value = user
    }

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = dataManager.isToolbarExpanded

    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        dataManager.isToolbarExpanded.value = expand
    }

    /** Logs out the currently signed in user */
    open fun logoutCurrentUser() {
        runBlocking {
            PlatformFirebase?.auth?.signOut()
            overrideCurrentUser(null)
            settings.remove(SETTINGS_KEY_AUTO_EMAIL)
            settings.remove(SETTINGS_KEY_AUTO_PASSWORD)
        }
    }

    /** Schedules a check for token expiration */
    private fun scheduleExpirationDate() {
        viewModelScope.launch {
            dataManager.overrideUser.value?.let { user ->
                delay(1000 * user.expiresAt.minus(Clock.System.now().epochSeconds).minus(EXPIRE_BOUNDARY))

                // Time's up! Let's bake a new pie! First, let's check no one stole our eggs, yet.
                if(user.expiresAt.minus(EXPIRE_BOUNDARY) <= Clock.System.now().epochSeconds) {
                    withContext(Dispatchers.IO) {
                        userOperationService.refreshToken(user.refreshToken)?.let {
                            overrideCurrentUser(
                                user.copy(
                                    refreshToken = it.refreshToken,
                                    expiresAt = Clock.System.now().epochSeconds + it.expiresIn,
                                    expiresIn = it.expiresIn,
                                    uid = it.userId
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}