package data.shared

import data.io.app.LocalSettings
import data.io.base.AppPing
import data.io.user.UserIO
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared data manager with most common information */
class SharedDataManager {

    /** Current configuration specific to this app */
    val localSettings = MutableStateFlow<LocalSettings?>(null)

    /** Information about current user including the token and its expiration */
    val currentUser = MutableStateFlow<UserIO?>(null)

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = MutableStateFlow(true)

    /** Acts as a sort of a in-app push notification, notifying of changes */
    val pingStream = MutableStateFlow(setOf<AppPing>())
}