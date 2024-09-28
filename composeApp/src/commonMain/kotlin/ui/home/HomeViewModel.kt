package ui.home

import androidx.lifecycle.ViewModel
import components.pull_refresh.RefreshableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    factory { HomeViewModel() }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel: ViewModel(), RefreshableViewModel {
    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {

    }
}