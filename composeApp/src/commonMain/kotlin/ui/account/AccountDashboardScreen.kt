package ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import base.BrandBaseScreen
import base.navigation.NavigationNode
import chat.enrichment.shared.ui.base.LocalNavController
import chat.enrichment.shared.ui.components.ErrorHeaderButton
import chat.enrichment.shared.ui.theme.LocalTheme
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.account_dashboard_fcm
import chatenrichment.composeapp.generated.resources.screen_account_title
import chatenrichment.composeapp.generated.resources.username_change_launcher_cancel
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
    val currentFcmToken = viewModel.currentFcmToken.collectAsState()


    LaunchedEffect(signOutResponse.value, currentUser.value) {
        if(signOutResponse.value && currentUser.value == null) {
            navController?.popBackStack(NavigationNode.Home, inclusive = false)
        }
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_account_title)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .fillMaxSize()
        ) {
            AnimatedVisibility(currentFcmToken.value != null) {
                Column {
                    Text(
                        stringResource(Res.string.account_dashboard_fcm),
                        style = LocalTheme.current.styles.category
                    )
                    SelectionContainer {
                        Text(
                            text = currentFcmToken.value ?: "",
                            style = LocalTheme.current.styles.title
                        )
                    }
                }
            }

            ErrorHeaderButton(
                modifier = Modifier
                    .padding(top = 32.dp)
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