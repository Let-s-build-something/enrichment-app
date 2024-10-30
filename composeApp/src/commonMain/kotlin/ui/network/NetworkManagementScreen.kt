package ui.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_list
import augmy.composeapp.generated.resources.network_received
import augmy.composeapp.generated.resources.screen_network_management
import augmy.composeapp.generated.resources.screen_network_new
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.components.rememberTabSwitchState
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavigationNode
import data.io.social.UserPrivacy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Screen for user managing their social network */
@Composable
fun NetworkManagementScreen(
    viewModel: NetworkReceivedViewModel = koinViewModel()
) {
    val currentUser = viewModel.currentUser.collectAsState()

    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        pageCount = {
            if(currentUser.value?.configuration?.privacy == UserPrivacy.PRIVATE) 2 else 1
        }
    )
    val switchThemeState = rememberTabSwitchState(
        tabs = mutableListOf(
            stringResource(Res.string.network_list),
            stringResource(Res.string.network_received)
        ),
        onSelectionChange = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(it)
            }
        }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            switchThemeState.selectedTabIndex.value = it
        }
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_network_management),
        actionIcons = { isExpanded ->
            ActionBarIcon(
                text = if(isExpanded) stringResource(Res.string.screen_network_new) else null,
                imageVector = Icons.AutoMirrored.Outlined.Send,
                onClick = {
                    navController?.navigate(NavigationNode.NetworkNew)
                }
            )
        }
    ) {
        Column {
            if(pagerState.pageCount > 1) {
                MultiChoiceSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = LocalTheme.current.shapes.screenCornerRadius,
                        topEnd = LocalTheme.current.shapes.screenCornerRadius
                    ),
                    state = switchThemeState
                )
            }
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState
            ) { index ->
                if(index == 0) {
                    NetworkListScreen()
                }else {
                    NetworkReceivedContent()
                }
            }
        }
    }
}