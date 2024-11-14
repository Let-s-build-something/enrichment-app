package ui.home

import data.io.user.NetworkItemIO
import kotlinx.coroutines.flow.MutableStateFlow

/** persistent manager of locally available data */
class HomeDataManager {
    
    /** currently downloaded network items */
    val networkItems = MutableStateFlow<List<NetworkItemIO?>?>(null)
}