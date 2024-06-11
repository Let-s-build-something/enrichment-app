package lets.build.chatenrichment.ui.login.username

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lets.build.chatenrichment.data.shared.io.firebase.FirebaseCollections
import lets.build.chatenrichment.data.shared.io.user.UserProfile
import javax.inject.Inject

/** Class for making DB requests */
class UsernameChangeRepository @Inject constructor() {

    /** Returns user profile based on uid */
    suspend fun saveUserProfile(uid: String, data: UserProfile): Task<Void> {
        return withContext(Dispatchers.IO) {
            Firebase.firestore
                .collection(FirebaseCollections.USERS.name)
                .document(uid)
                .set(data)
        }
    }
}