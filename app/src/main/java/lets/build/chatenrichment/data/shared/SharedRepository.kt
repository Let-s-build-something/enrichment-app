package lets.build.chatenrichment.data.shared

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lets.build.chatenrichment.data.shared.io.firebase.FirebaseCollections
import javax.inject.Inject

/** Class for making DB requests */
class SharedRepository @Inject constructor() {

    /** Returns user profile based on uid */
    suspend fun getUserProfile(uid: String): Task<DocumentSnapshot> {
        return withContext(Dispatchers.IO) {
            Firebase.firestore
                .collection(FirebaseCollections.USERS.name)
                .document(uid)
                .get()
        }
    }
}