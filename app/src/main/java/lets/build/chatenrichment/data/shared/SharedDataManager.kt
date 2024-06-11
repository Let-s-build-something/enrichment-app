package lets.build.chatenrichment.data.shared

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import lets.build.chatenrichment.data.shared.io.user.UserProfile
import javax.inject.Inject

/** Shared data manager with most common information */
@ActivityRetainedScoped
class SharedDataManager @Inject constructor() {

    /** currently signed in user */
    val currentUser = MutableStateFlow(Firebase.auth.currentUser)

    /** All user's information */
    val userProfile = MutableStateFlow<UserProfile?>(null)
}