package ui.login.username

import androidx.lifecycle.viewModelScope
import data.shared.SharedDataManager
import data.shared.SharedViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch

class UsernameChangeViewModel (
    private val repository: UsernameChangeRepository
): SharedViewModel() {

    /** Logs out the currently signed in user */
    fun saveName(name: String) {
        viewModelScope.launch {

        }
    }
}