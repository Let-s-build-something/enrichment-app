package lets.build.chatenrichment.data.shared

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/** Shared data manager with most common information */
class SharedDataManager @Inject constructor() {

    /** currently signed in user */
    val currentUser = MutableStateFlow(Firebase.auth.currentUser)
}