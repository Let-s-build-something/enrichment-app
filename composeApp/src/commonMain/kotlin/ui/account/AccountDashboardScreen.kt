package ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import base.BrandBaseScreen
import chat.enrichment.shared.ui.base.LocalNavController
import base.navigation.NavigationNode
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.screen_account_title
import chatenrichment.composeapp.generated.resources.username_change_launcher_cancel
import chat.enrichment.shared.ui.components.ErrorHeaderButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Screen for the home page
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun AccountDashboardScreen(viewModel: AccountDashboardViewModel = koinViewModel()) {
    val navController = LocalNavController.current

    val currentUser = viewModel.currentUser.collectAsState(null)
    val signOutResponse = viewModel.signOutResponse.collectAsState()


    LaunchedEffect(signOutResponse.value, currentUser.value) {
        if(signOutResponse.value && currentUser.value == null) {
            navController?.popBackStack(NavigationNode.Home, inclusive = false)
        }
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_account_title)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ErrorHeaderButton(
                modifier = Modifier
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                text = stringResource(Res.string.username_change_launcher_cancel),
                endIconVector = Icons.AutoMirrored.Outlined.Logout,
                onClick = {
                    viewModel.logoutCurrentUser()
                }
            )
        }
    }
}