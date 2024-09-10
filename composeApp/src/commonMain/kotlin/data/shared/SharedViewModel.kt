package data.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform

/** Viewmodel with shared behavior and injections for general purposes */
open class SharedViewModel: ViewModel() {

    /** Singleton data manager to keep session-only data alive */
    protected val dataManager: SharedDataManager = KoinPlatform.getKoin().get()

    /** persistent settings saved locally to a device */
    protected val settings = KoinPlatform.getKoin().get<Settings>()


    /** currently signed in user */
    val currentUser = Firebase.auth.authStateChanged
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            //Smart cast to 'dev.gitlive.firebase.Firebase' is impossible, because 'PlatformFirebase' is a expect property.
            Firebase.auth.currentUser
        )

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = dataManager.isToolbarExpanded

    /** Changes the state of the toolbar */
    fun changeToolbarState(expand: Boolean) {
        dataManager.isToolbarExpanded.value = expand
    }

    /** Logs out the currently signed in user */
    open fun logoutCurrentUser() {
        runBlocking {
            Firebase.auth.signOut()
        }
    }
}