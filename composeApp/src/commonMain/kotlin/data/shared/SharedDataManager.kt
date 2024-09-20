package data.shared

import data.io.user.UserIO
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared data manager with most common information */
class SharedDataManager {

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = MutableStateFlow(true)

    /** Information about current user including the token and its expiration */
    val currentUser = MutableStateFlow<UserIO?>(null)
}