package ui.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_list
import augmy.composeapp.generated.resources.network_received
import augmy.composeapp.generated.resources.screen_network_management
import augmy.composeapp.generated.resources.screen_network_new
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import base.BrandBaseScreen
import data.io.social.UserPrivacy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.add_new.NetworkAddNewLauncher
import ui.network.list.NetworkListContent
import ui.network.received.NetworkReceivedContent
import ui.network.received.NetworkReceivedViewModel

/** Screen for user managing their social network */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkManagementScreen(
    displayName: String? = null,
    tag: String? = null,
    viewModel: NetworkReceivedViewModel = koinViewModel()
) {
    val currentUser = viewModel.currentUser.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val selectedTabIndex = rememberSaveable {
        mutableStateOf(0)
    }
    val showAddNewModal = rememberSaveable {
        mutableStateOf(displayName != null || tag != null)
    }

    val pagerState = rememberPagerState(
        pageCount = {
            if(currentUser.value?.configuration?.privacy != UserPrivacy.Public) 2 else 1
        },
        initialPage = selectedTabIndex.value
    )
    val switchThemeState = rememberMultiChoiceState(
        tabs = mutableListOf(
            stringResource(Res.string.network_list),
            stringResource(Res.string.network_received)
        ),
        onSelectionChange = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        selectedTabIndex = selectedTabIndex
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            switchThemeState.selectedTabIndex.value = it
        }
    }

    if(showAddNewModal.value) {
        NetworkAddNewLauncher(
            displayName = displayName,
            tag = tag,
            onDismissRequest = {
                showAddNewModal.value = false
            }
        )
    }

    BrandBaseScreen(
        title = stringResource(Res.string.screen_network_management),
        actionIcons = { isExpanded ->
            ActionBarIcon(
                text = if(isExpanded) stringResource(Res.string.screen_network_new) else null,
                imageVector = Icons.AutoMirrored.Outlined.Send,
                onClick = {
                    showAddNewModal.value = true
                }
            )
        }
    ) {
        Column {
            if(pagerState.pageCount > 1) {
                MultiChoiceSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    state = switchThemeState
                )
            }
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
                beyondViewportPageCount = 1
            ) { index ->
                if(index == 0) {
                    NetworkListContent(
                        openAddNewModal = {
                            showAddNewModal.value = true
                        }
                    )
                }else {
                    NetworkReceivedContent()
                }
            }
        }
    }
}