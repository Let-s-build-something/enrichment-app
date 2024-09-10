package data.shared

import data.io.CloudUser
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared data manager with most common information */
class SharedDataManager {

    /** Override of the user object and flow */
    val overrideUser = MutableStateFlow<CloudUser?>(null)

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = MutableStateFlow(true)
}