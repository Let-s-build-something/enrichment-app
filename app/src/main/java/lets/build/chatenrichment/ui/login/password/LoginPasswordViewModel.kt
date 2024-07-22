package lets.build.chatenrichment.ui.login.password

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squadris.squadris.compose.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lets.build.chatenrichment.data.shared.SharedDataManager
import javax.inject.Inject

@HiltViewModel
class LoginPasswordViewModel @Inject constructor(
    private val sharedDataManager: SharedDataManager
): BaseViewModel() {

    /** currently signed in user */
    val currentUser = sharedDataManager.currentUser.asStateFlow()

    /** Requests signup with an email and a password */
    fun signUpWithPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            Firebase.auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sharedDataManager.currentUser.value = Firebase.auth.currentUser
                        Log.d("kostka_test", "createUserWithEmail:success, user: ${sharedDataManager.currentUser.value}")
                    } else {
                        Log.w("kostka_test", "createUserWithEmail:failure", task.exception)
                    }
                }
        }
    }
}