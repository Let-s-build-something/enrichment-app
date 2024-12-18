package data.shared

import data.io.app.LocalSettings
import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/** Shared data manager with most common information */
class SharedDataManager {

    /** Current configuration specific to this app */
    val localSettings = MutableStateFlow<LocalSettings?>(null)

    /** Information about current user including the token and its expiration */
    val mutableUser = MutableStateFlow<UserIO?>(null)

    /** Information about current user including the token and its expiration */
    val currentUser = mutableUser.combine(
        Firebase.auth.idTokenChanged
    ) { user, firebaseUser ->
        withContext(Dispatchers.IO) {
            println("kostka_test, requesting new idToken")
            user?.copy(idToken = firebaseUser?.getIdToken(false))
        }
    }

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = MutableStateFlow(true)
}