package ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.network_dialog_message_block
import augmy.composeapp.generated.resources.network_dialog_message_mute
import augmy.composeapp.generated.resources.network_dialog_title_block
import augmy.composeapp.generated.resources.network_dialog_title_mute
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.getOrNull
import components.EmptyLayout
import components.HorizontalScrollChoice
import components.OptionsLayout
import components.OptionsLayoutAction
import components.ScrollChoice
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableScreen
import data.BlockedProximityValue
import data.NetworkProximityCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.add_new.NetworkAddNewLauncher
import ui.network.list.NETWORK_SHIMMER_ITEM_COUNT
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Screen for the home page
 */
@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val coroutineScope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val density = LocalDensity.current

    val networkItems = viewModel.networkItems.collectAsLazyPagingItems()
    val categories = viewModel.categories.collectAsState(initial = listOf())
    val customColors = viewModel.customColors.collectAsState(initial = mapOf())
    val isLoadingInitialPage = networkItems.loadState.refresh is LoadState.Loading
    val isEmpty = networkItems.itemCount == 0 && networkItems.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage

    val listState = rememberLazyGridState()
    val stickyHeaderHeight = rememberSaveable { mutableStateOf(0f) }
    val showTuner = rememberSaveable { mutableStateOf(false) }
    val isCompactView = rememberSaveable { mutableStateOf(true) }
    val checkedItems = remember { mutableStateListOf<String>() }
    val showAddNewModal = rememberSaveable {
        mutableStateOf(false)
    }
    val showActionDialog = rememberSaveable {
        mutableStateOf<OptionsLayoutAction?>(null)
    }

    val onAction: (OptionsLayoutAction) -> Unit = { action ->
        when(action) {
            OptionsLayoutAction.Mute -> {
                showActionDialog.value = OptionsLayoutAction.Mute
            }
            OptionsLayoutAction.Block -> {
                showActionDialog.value = OptionsLayoutAction.Block
            }
            OptionsLayoutAction.CircleMove -> {
                // TODO
            }
            OptionsLayoutAction.DeselectAll -> checkedItems.clear()
            OptionsLayoutAction.SelectAll -> {
                coroutineScope.launch(Dispatchers.Default) {
                    checkedItems.addAll(
                        checkedItems.toMutableSet().apply {
                            addAll(networkItems.itemSnapshotList.items.mapNotNull { it.userPublicId })
                        }
                    )
                }
            }
        }
    }

    showActionDialog.value?.let { action ->
        AlertDialog(
            title = stringResource(
                if(action == OptionsLayoutAction.Mute) {
                    Res.string.network_dialog_title_mute
                }else Res.string.network_dialog_title_block
            ),
            message = stringResource(
                if(action == OptionsLayoutAction.Mute) {
                    Res.string.network_dialog_message_mute
                }else Res.string.network_dialog_message_block
            ),
            icon = action.leadingImageVector,
            confirmButtonState = ButtonState(
                text = stringResource(Res.string.button_confirm)
            ) {
                viewModel.requestProximityChange(
                    selectedConnections = checkedItems,
                    proximity = if(action == OptionsLayoutAction.Mute) {
                        NetworkProximityCategory.Public.range.start
                    }else BlockedProximityValue,
                    onOperationDone = {
                        networkItems.refresh()
                    }
                )
                checkedItems.clear()
            },
            dismissButtonState = ButtonState(
                text = stringResource(Res.string.button_dismiss)
            ),
            onDismissRequest = {
                showActionDialog.value = null
            }
        )
    }

    if(showTuner.value) {
        NetworkPreferencesLauncher(
            viewModel = viewModel,
            onDismissRequest = {
                showTuner.value = false
            }
        )
    }else if(showAddNewModal.value) {
        NetworkAddNewLauncher(onDismissRequest = {
            showAddNewModal.value = false
        })
    }

    RefreshableScreen(
        title = stringResource(Res.string.screen_home),
        navIconType = NavIconType.TUNE,
        onNavigationIconClick = {
            showTuner.value = true
        },
        onRefresh = {
            networkItems.refresh()
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        },
        showDefaultActions = true,
        viewModel = viewModel,
        actionIcons = { isExpanded ->
            ActionBarIcon(
                text = if(isExpanded) stringResource(Res.string.screen_search_network) else null,
                imageVector = Icons.Outlined.Search,
                onClick = {
                    navController?.navigate(NavigationNode.SearchNetwork)
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            HorizontalScrollChoice(
                modifier = Modifier
                    .zIndex(2f)
                    .fillMaxWidth(),
                choices = NetworkProximityCategory.entries.map {
                    ScrollChoice(
                        id = it,
                        text = stringResource(it.res)
                    )
                },
                onSelectionChange = { item, isSelected ->
                    checkedItems.clear()
                    val newList = categories.value.toMutableList()
                    if(isSelected) {
                        newList.add(item)
                    }else {
                        newList.removeAll { it == item }
                    }
                    if(newList.isEmpty()) {
                        newList.add(if(item == NetworkProximityCategory.Family) {
                            NetworkProximityCategory.Peers
                        }else NetworkProximityCategory.Family)
                    }
                    viewModel.filterNetworkItems(filter = newList)
                },
                selectedItems = categories.value
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                OptionsLayout(
                    modifier = Modifier
                        .zIndex(1f)
                        .onSizeChanged {
                            stickyHeaderHeight.value = with(density) {
                                it.height.toFloat().toDp().value
                            }
                        },
                    show = checkedItems.size > 0,
                    onClick = onAction
                )
                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier.align(Alignment.TopEnd).zIndex(1f),
                    visible = checkedItems.size == 0 && !isEmpty
                ) {
                    Crossfade(
                        modifier = Modifier.zIndex(1f),
                        targetState = isCompactView.value
                    ) { isList ->
                        MinimalisticFilledIcon(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .zIndex(1f),
                            imageVector = if(isList) Icons.Outlined.TrackChanges else Icons.AutoMirrored.Outlined.List,
                            onTap = {
                                isCompactView.value = !isCompactView.value
                            }
                        )
                    }
                }
                Crossfade(isCompactView.value) { isList ->
                    if(isList) {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        coroutineScope.launch {
                                            listState.scrollBy(-delta)
                                        }
                                    }
                                )
                                .fillMaxSize(),
                            columns = GridCells.Fixed(
                                if(LocalDeviceType.current == WindowWidthSizeClass.Compact) 1 else 2
                            ),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))
                                    AnimatedVisibility(checkedItems.size > 0) {
                                        Spacer(
                                            Modifier
                                                .padding(top = LocalTheme.current.shapes.betweenItemsSpace)
                                                .height(stickyHeaderHeight.value.dp - LocalTheme.current.shapes.betweenItemsSpace)
                                                .animateContentSize()
                                        )
                                    }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                androidx.compose.animation.AnimatedVisibility(
                                    enter = expandVertically() + fadeIn(),
                                    visible = isEmpty
                                ) {
                                    EmptyLayout(
                                        title = stringResource(Res.string.network_list_empty_title),
                                        action = stringResource(Res.string.network_list_empty_action),
                                        onClick = {
                                            showAddNewModal.value = true
                                        }
                                    )
                                }
                            }
                            items(
                                count = if(networkItems.itemCount == 0 && isLoadingInitialPage) NETWORK_SHIMMER_ITEM_COUNT else networkItems.itemCount,
                                key = { index -> networkItems.getOrNull(index)?.userPublicId ?: Uuid.random().toString() }
                            ) { index ->
                                networkItems.getOrNull(index).let { data ->
                                    Column(modifier = Modifier.animateItem()) {
                                        NetworkItemRow(
                                            data = data,
                                            isChecked = if(checkedItems.size > 0) checkedItems.contains(data?.userPublicId) else null,
                                            color = NetworkProximityCategory.entries.firstOrNull {
                                                it.range.contains(data?.proximity ?: 1f)
                                            }.let {
                                                customColors.value[it] ?: it?.color
                                            },
                                            onCheckChange = { isLongClick ->
                                                when {
                                                    checkedItems.contains(data?.userPublicId) -> checkedItems.remove(data?.userPublicId)
                                                    isLongClick || checkedItems.size > 0 -> {
                                                        data?.userPublicId?.let { publicId ->
                                                            checkedItems.add(publicId)
                                                        }
                                                    }
                                                    else -> navController?.navigate(
                                                        NavigationNode.Conversation(
                                                            conversationId = data?.userPublicId,
                                                            name = data?.displayName
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                        if(index != networkItems.itemCount - 1) {
                                            Divider(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = LocalTheme.current.colors.disabledComponent,
                                                thickness = .3.dp
                                            )
                                        }
                                    }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(
                                    Modifier
                                        .padding(WindowInsets.navigationBars.asPaddingValues())
                                        .height(LocalTheme.current.shapes.betweenItemsSpace * 2)
                                )
                            }
                        }
                    }else {
                        SocialCircleContent(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel,
                            headerHeightDp = stickyHeaderHeight.value
                        )
                    }
                }
            }
        }
    }
}
