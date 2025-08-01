package ui.network.received

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_save
import augmy.composeapp.generated.resources.network_request_accepted
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.utils.getOrNull
import components.network.CircleRequestRow
import components.pull_refresh.RefreshableContent
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.NetworkProximityCategory
import data.io.base.BaseResponse
import data.io.user.NetworkItemIO
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.RefreshHandler
import ui.network.components.ProximityPicker
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Layout displaying currently pending requests for inclusion to social circles */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun NetworkReceivedContent(
    viewModel: NetworkReceivedModel = koinViewModel(),
    refreshHandler: RefreshHandler
) {
    val requests = viewModel.requests.collectAsLazyPagingItems()
    val response = viewModel.response.collectAsState()
    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val snackbarHostState = LocalSnackbarHost.current
    val isLoadingInitialPage = requests.loadState.refresh is LoadState.Loading

    val showProximityPicker = remember {
        mutableStateOf<NetworkItemIO?>(null)
    }

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

    LaunchedEffect(Unit) {
        refreshHandler.addListener {
            viewModel.requestData(isSpecial = true, isPullRefresh = true)
            requests.refresh()
        }
    }

    showProximityPicker.value?.let { networkItem ->
        ProximityPickerModal(
            networkItem = networkItem,
            onSelection = { proximity ->
                viewModel.acceptRequest(
                    publicId = networkItem.publicId,
                    proximity = proximity,
                    networkItem = networkItem
                )
            },
            onDismissRequest = {
                showProximityPicker.value = null
            }
        )
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
                    /*EmptyLayout(
                        title = stringResource(Res.string.network_received_empty_title),
                        description = stringResource(Res.string.network_received_empty_description),
                        secondaryAction = stringResource(Res.string.network_received_empty_action),
                        onClick = {
                            shareProfile(
                                publicId = viewModel.currentUser.value?.publicId,
                                snackbarHostState = snackbarHostState,
                                clipboard = clipboard,
                                coroutineScope = coroutineScope
                            )
                        }
                    )*/
                }
            }
            items(
                count = if(requests.itemCount == 0 && isLoadingInitialPage) SHIMMER_ITEM_COUNT else requests.itemCount,
                key = { index -> requests.getOrNull(index)?.publicId ?: Uuid.random().toString() }
            ) { index ->
                requests.getOrNull(index).let { data ->
                    CircleRequestRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        data = data,
                        response = response.value[data?.publicId],
                        onResponse = { accept ->
                            if(data?.publicId != null) {
                                if(accept) {
                                    showProximityPicker.value = NetworkItemIO(
                                        publicId = data.publicId,
                                        avatar = data.avatar,
                                        displayName = data.displayName
                                    )
                                }else viewModel.acceptRequest(publicId = data.publicId, proximity = null)
                            }
                        }
                    )
                    if(requests.itemCount - 1 != index) {
                        HorizontalDivider(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProximityPickerModal(
    networkItem: NetworkItemIO,
    onSelection: (proximity: Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    val selectedCategory = remember {
        mutableStateOf(NetworkProximityCategory.Public)
    }

    SimpleModalBottomSheet(
        onDismissRequest = onDismissRequest,
        scrollEnabled = false,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProximityPicker(
            model = koinViewModel(),
            selectedCategory = selectedCategory,
            newItem = networkItem
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, alignment = Alignment.End)
        ) {
            OutlinedButton(
                text = stringResource(Res.string.accessibility_cancel),
                onClick = onDismissRequest,
                activeColor = SharedColors.RED_ERROR_50
            )
            OutlinedButton(
                text = stringResource(Res.string.accessibility_save),
                onClick = {
                    onSelection(selectedCategory.value.range.start)
                },
                activeColor = LocalTheme.current.colors.brandMain
            )
        }
    }
}

private const val SHIMMER_ITEM_COUNT = 10
