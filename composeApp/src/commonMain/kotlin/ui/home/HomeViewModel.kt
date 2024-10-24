package ui.home

import androidx.lifecycle.viewModelScope
import components.pull_refresh.RefreshableViewModel
import data.shared.SharedRepository
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

internal val homeModule = module {
    factory { HomeViewModel() }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel: SharedViewModel(), RefreshableViewModel {

    /** main shared repository */
    protected val sharedRepository: SharedRepository = KoinPlatform.getKoin().get()

    init {
        viewModelScope.launch {
            sharedDataManager.currentUser.value = sharedRepository.authenticateUser()
        }
    }

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {

    }
}