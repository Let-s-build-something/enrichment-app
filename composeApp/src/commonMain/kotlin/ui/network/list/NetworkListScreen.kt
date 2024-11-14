package ui.network.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.theme.LocalTheme
import base.getOrNull
import base.navigation.NavigationArguments
import collectResult
import components.EmptyLayout
import components.OptionsLayout
import components.OptionsLayoutAction
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableContent
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Screen containing current user's network and offers its management */
@OptIn(ExperimentalUuidApi::class, ExperimentalFoundationApi::class)
@Composable
fun NetworkListContent(
    openAddNewModal: () -> Unit,
    viewModel: NetworkListViewModel = koinViewModel()
) {
    val networkItems = viewModel.requests.collectAsLazyPagingItems()
    val response = viewModel.response.collectAsState()
    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val navController = LocalNavController.current
    val isLoadingInitialPage = networkItems.loadState.refresh is LoadState.Loading

    val checkedItems = remember { mutableStateListOf<String?>() }
    val selectedItem = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val onAction: (OptionsLayoutAction) -> Unit = { action ->
        when(action) {
            OptionsLayoutAction.AddTo -> {
                // TODO
            }
            OptionsLayoutAction.Block -> {
                // TODO
            }
            OptionsLayoutAction.CircleMove -> {
                // TODO
            }
            OptionsLayoutAction.DeselectAll -> checkedItems.clear()
            OptionsLayoutAction.SelectAll -> {
                coroutineScope.launch(Dispatchers.Default) {
                    checkedItems.addAll(
                        checkedItems.toMutableSet().apply {
                            addAll(networkItems.itemSnapshotList.items.map { it.publicId })
                        }
                    )
                }
            }
        }
    }


    navController?.collectResult(
        key = NavigationArguments.NETWORK_NEW_SUCCESS,
        defaultValue = false,
        listener = { isSuccess ->
            if(isSuccess) networkItems.refresh()
        }
    )

    RefreshableContent(
        onRefresh = {
            viewModel.requestData(isSpecial = true, isPullRefresh = true)
            networkItems.refresh()
        },
        isRefreshing = isRefreshing.value
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            stickyHeader {
                OptionsLayout(
                    modifier = Modifier.animateItem(),
                    show = checkedItems.size > 0,
                    onClick = onAction
                )
            }
            item {
                AnimatedVisibility(
                    enter = expandVertically() + fadeIn(),
                    visible = networkItems.itemCount == 0 && !isLoadingInitialPage
                ) {
                    EmptyLayout(
                        title = stringResource(Res.string.network_list_empty_title),
                        action = stringResource(Res.string.network_list_empty_action),
                        onClick = openAddNewModal
                    )
                }
            }
            items(
                count = if(networkItems.itemCount == 0 && isLoadingInitialPage) NETWORK_SHIMMER_ITEM_COUNT else networkItems.itemCount,
                key = { index -> networkItems.getOrNull(index)?.publicId ?: Uuid.random().toString() }
            ) { index ->
                networkItems.getOrNull(index).let { data ->
                    NetworkItemRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        isChecked = if(checkedItems.size > 0) {
                            checkedItems.contains(data?.publicId)
                        }else null,
                        data = data,
                        response = response.value[data?.publicId],
                        onAction = onAction,
                        isSelected = selectedItem.value == data?.publicId,
                        onCheckChange = { isLongClick ->
                            when {
                                checkedItems.contains(data?.publicId) -> checkedItems.remove(data?.publicId)
                                isLongClick || checkedItems.size > 0 -> {
                                    selectedItem.value = null
                                    checkedItems.add(data?.publicId)
                                }
                                else -> {
                                    selectedItem.value = if(selectedItem.value == data?.publicId) null else data?.publicId
                                }
                            }
                        }
                    )
                    if(networkItems.itemCount - 1 != index) {
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

/** Number of network items within one screen to be shimmered */
const val NETWORK_SHIMMER_ITEM_COUNT = 20