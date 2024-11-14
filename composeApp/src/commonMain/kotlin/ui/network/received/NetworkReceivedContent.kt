package ui.network.received

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_received_empty_action
import augmy.composeapp.generated.resources.network_received_empty_description
import augmy.composeapp.generated.resources.network_received_empty_title
import augmy.composeapp.generated.resources.network_request_accepted
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.theme.LocalTheme
import base.getOrNull
import components.EmptyLayout
import components.network.CircleSentRequestRow
import components.pull_refresh.RefreshableContent
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.io.base.BaseResponse
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.account.shareProfile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Layout displaying currently pending requests for inclusion to social circles */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun NetworkReceivedContent(viewModel: NetworkReceivedViewModel = koinViewModel()) {
    val requests = viewModel.requests.collectAsLazyPagingItems()
    val response = viewModel.response.collectAsState()
    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val snackbarHostState = LocalSnackbarHost.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val isLoadingInitialPage = requests.loadState.refresh is LoadState.Loading

    LaunchedEffect(Unit) {
        viewModel.response.collectLatest { res ->
            if(res.any { it.value is BaseResponse.Success }) {
                requests.refresh()
                snackbarHostState?.showSnackbar(
                    message = getString(Res.string.network_request_accepted)
                )
            }
        }
    }

    RefreshableContent(
        onRefresh = {
            viewModel.requestData(isSpecial = true, isPullRefresh = true)
            requests.refresh()
        },
        isRefreshing = isRefreshing.value
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                AnimatedVisibility(
                    enter = expandVertically() + fadeIn(),
                    visible = requests.itemCount == 0 && !isLoadingInitialPage
                ) {
                    EmptyLayout(
                        title = stringResource(Res.string.network_received_empty_title),
                        description = stringResource(Res.string.network_received_empty_description),
                        action = stringResource(Res.string.network_received_empty_action),
                        onClick = {
                            shareProfile(
                                publicId = viewModel.currentUser.value?.publicId,
                                snackbarHostState = snackbarHostState,
                                clipboardManager = clipboardManager,
                                coroutineScope = coroutineScope
                            )
                        }
                    )
                }
            }
            items(
                count = if(requests.itemCount == 0 && isLoadingInitialPage) SHIMMER_ITEM_COUNT else requests.itemCount,
                key = { index -> requests.getOrNull(index)?.uid ?: Uuid.random().toString() }
            ) { index ->
                requests.getOrNull(index).let { data ->
                    CircleSentRequestRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        data = data,
                        response = response.value[data?.uid],
                        onResponse = { accept ->
                            if(data?.uid != null) viewModel.acceptRequest(uid = data.uid, accept = accept)
                        }
                    )
                    if(requests.itemCount - 1 != index) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = LocalTheme.current.colors.disabledComponent,
                            thickness = .3.dp
                        )
                    }
                }
            }
        }
    }
}

private const val SHIMMER_ITEM_COUNT = 10