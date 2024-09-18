package data.shared

import kotlinx.coroutines.flow.MutableStateFlow

/** Shared data manager with most common information */
class SharedDataManager {

    /** whether toolbar is currently expanded */
    val isToolbarExpanded = MutableStateFlow(true)
}