package components.pull_refresh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** ViewModel containing behavior for refreshing data */
interface RefreshableViewModel {

    companion object {
        /** minimum amount of millis required for refresh to take place */
        const val MINIMUM_REFRESH_DELAY = 2000L

        /** requests data from the ViewModel */
        fun ViewModel.requestData(isSpecial: Boolean, isPullRefresh: Boolean = false) {
            if(this is RefreshableViewModel) {
                this.requestData(viewModelScope, isSpecial, isPullRefresh)
            }
        }
    }

    /** whether current data is refreshing or not */
    val isRefreshing: MutableStateFlow<Boolean>

    /** time in millisecond of the last refresh */
    var lastRefreshTimeMillis: Long

    /** requests data from the ViewModel */
    fun requestData(
        scope: CoroutineScope,
        isSpecial: Boolean,
        isPullRefresh: Boolean = false
    ) {
        scope.launch {
            if(isPullRefresh) setRefreshing(true)
            onDataRequest(isSpecial, isPullRefresh)
            if(isPullRefresh) setRefreshing(false)
        }
    }

    /** sets the value or refreshing */
    suspend fun CoroutineScope.setRefreshing(refreshing: Boolean) {
        if(refreshing) {
            lastRefreshTimeMillis = Clock.System.now().toEpochMilliseconds()
            isRefreshing.value = true
        }else {
            delay(kotlin.math.max(
                Clock.System.now().toEpochMilliseconds().minus(lastRefreshTimeMillis),
                MINIMUM_REFRESH_DELAY
            ))
            isRefreshing.value = false
        }
    }

    /** requests for completely new data batch */
    suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean = false)
}