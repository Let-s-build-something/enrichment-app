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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import base.BrandBaseScreen
import base.navigation.NavigationNode
import chat.enrichment.shared.ui.base.LocalNavController
import chat.enrichment.shared.ui.components.ErrorHeaderButton
import chat.enrichment.shared.ui.components.MultiChoiceSwitch
import chat.enrichment.shared.ui.components.rememberTabSwitchState
import chat.enrichment.shared.ui.theme.LocalTheme
import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.account_dashboard_fcm
import chatenrichment.composeapp.generated.resources.account_dashboard_theme
import chatenrichment.composeapp.generated.resources.account_dashboard_theme_dark
import chatenrichment.composeapp.generated.resources.account_dashboard_theme_device
import chatenrichment.composeapp.generated.resources.account_dashboard_theme_light
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

    val currentUser = viewModel.firebaseUser.collectAsState(null)
    val signOutResponse = viewModel.signOutResponse.collectAsState()
    val localSettings = viewModel.localSettings.collectAsState()

    val switchThemeState = rememberTabSwitchState(
        tabs = mutableListOf(
            stringResource(Res.string.account_dashboard_theme_light),
            stringResource(Res.string.account_dashboard_theme_dark),
            stringResource(Res.string.account_dashboard_theme_device)
        ),
        selectedTabIndex = mutableStateOf(localSettings.value?.theme?.ordinal ?: 0),
        onSelectionChange = {
            viewModel.updateTheme(it)
        }
    )

    LaunchedEffect(localSettings.value?.theme) {
        switchThemeState.selectedTabIndex.value = localSettings.value?.theme?.ordinal ?: 0
    }

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
            Text(
                stringResource(Res.string.account_dashboard_theme),
                style = LocalTheme.current.styles.category
            )
            MultiChoiceSwitch(
                modifier = Modifier.fillMaxWidth(),
                shape = LocalTheme.current.shapes.rectangularActionShape,
                state = switchThemeState
            )

            AnimatedVisibility(
                modifier = Modifier.padding(top = LocalTheme.current.shapes.betweenItemsSpace),
                visible = localSettings.value?.fcmToken != null
            ) {
                Column {
                    Text(
                        stringResource(Res.string.account_dashboard_fcm),
                        style = LocalTheme.current.styles.category
                    )
                    SelectionContainer {
                        Text(
                            text = localSettings.value?.fcmToken ?: "",
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