package ui.home

import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    factory { HomeViewModel() }
    viewModelOf(::HomeViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class HomeViewModel: ViewModel() {

}