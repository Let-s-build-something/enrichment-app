package ui.home

import data.io.user.NetworkItemIO
import kotlinx.coroutines.flow.MutableStateFlow

class HomeDataManager {
    /** Lit of network items */
    val networkItems = MutableStateFlow<List<NetworkItemIO>?>(null)
}