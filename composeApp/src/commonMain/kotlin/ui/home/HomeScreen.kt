package ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_home
import augmy.interactive.shared.ui.base.LocalNavController
import base.navigation.NavIconType
import base.navigation.NavigationNode
import components.pull_refresh.RefreshableScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for the home page
 */
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
            val navController = LocalNavController.current

            LaunchedEffect(Unit) {
                navController?.navigate(
                    NavigationNode.UserProfile()
                )
            }
        }
    }
}