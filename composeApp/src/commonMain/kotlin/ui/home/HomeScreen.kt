package ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.MinimalisticBrandIcon
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.getOrNull
import base.navigation.NavIconType
import base.navigation.NavigationNode
import components.EmptyLayout
import components.HorizontalScrollChoice
import components.OptionsLayout
import components.OptionsLayoutAction
import components.ScrollChoice
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableScreen
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

    val stickyHeaderHeight = rememberSaveable { mutableStateOf(0f) }
    val showTuner = rememberSaveable { mutableStateOf(false) }
    val isCompactView = rememberSaveable { mutableStateOf(true) }
    val checkedItems = remember { mutableStateListOf<String?>() }
    val showAddNewModal = rememberSaveable {
        mutableStateOf(false)
    }

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
                    visible = checkedItems.size == 0 && !(networkItems.itemCount == 0 && !isLoadingInitialPage)
                ) {
                    Crossfade(
                        modifier = Modifier.zIndex(1f),
                        targetState = isCompactView.value
                    ) { isList ->
                        MinimalisticBrandIcon(
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
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(
                                if(LocalDeviceType.current == WindowWidthSizeClass.Compact) 1 else 2
                            ),
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
                                    visible = networkItems.itemCount == 0 && !isLoadingInitialPage
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
                                key = { index -> networkItems.getOrNull(index)?.publicId ?: Uuid.random().toString() }
                            ) { index ->
                                networkItems.getOrNull(index).let { data ->
                                    Column(modifier = Modifier.animateItem()) {
                                        NetworkItemRow(
                                            data = data,
                                            isChecked = if(checkedItems.size > 0) checkedItems.contains(data?.publicId) else null,
                                            color = NetworkProximityCategory.entries.firstOrNull {
                                                it.range.contains(data?.proximity ?: 1f)
                                            }.let {
                                                customColors.value[it] ?: it?.color
                                            },
                                            onCheckChange = { isLongClick ->
                                                when {
                                                    checkedItems.contains(data?.publicId) -> checkedItems.remove(data?.publicId)
                                                    isLongClick || checkedItems.size > 0 -> {
                                                        checkedItems.add(data?.publicId)
                                                    }
                                                    else -> navController?.navigate(
                                                        NavigationNode.Conversation(userPublicId = data?.publicId)
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
                            item {
                                Spacer(
                                    Modifier
                                        .padding(WindowInsets.statusBars.asPaddingValues())
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
