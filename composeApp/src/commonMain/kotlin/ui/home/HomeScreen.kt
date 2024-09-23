package ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import base.navigation.NavIconType
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.screen_home
import components.pull_refresh.RefreshableScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Screen for the home page
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    RefreshableScreen(
        title = stringResource(Res.string.screen_home),
        navIconType = NavIconType.HOME,
        viewModel = viewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

        }
    }
}