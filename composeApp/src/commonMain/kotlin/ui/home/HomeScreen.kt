package ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavIconType
import base.navigation.NavigationNode
import components.HorizontalScrollChoice
import components.OptionsLayout
import components.OptionsLayoutAction
import components.ScrollChoice
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableScreen
import data.NetworkProximityCategory
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.list.NETWORK_SHIMMER_ITEM_COUNT
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Screen for the home page
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val networkItems = viewModel.networkItems.collectAsState()

    val stickyHeaderHeight = rememberSaveable {
        mutableStateOf(0f)
    }
    val categoryChoices = remember {
        mutableStateListOf(
            *viewModel.defaultChoices?.toTypedArray() ?: arrayOf(
                NetworkProximityCategory.Family,
                NetworkProximityCategory.Peers
            )
        )
    }
    val checkedItems = remember { mutableStateListOf<String?>() }

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
                            addAll(networkItems.value?.map { it?.publicId }.orEmpty())
                        }
                    )
                }
            }
        }
    }

    RefreshableScreen(
        title = stringResource(Res.string.screen_home),
        navIconType = NavIconType.TUNE,
        onNavigationIconClick = {
            // TODO bottom sheet with tune options?
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
                    if(isSelected) {
                        categoryChoices.add(item)
                    }else {
                        categoryChoices.removeAll { it == item }
                    }
                    if(categoryChoices.isEmpty()) {
                        categoryChoices.add(if(item == NetworkProximityCategory.Family) {
                            NetworkProximityCategory.Peers
                        }else NetworkProximityCategory.Family)
                    }
                    viewModel.filterNetworkItems(categories = categoryChoices)
                },
                selectedItems = categoryChoices
            )
            Box(
                modifier = Modifier.weight(1f, fill = true)
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
                                        .animateContentSize()
                                        .height(stickyHeaderHeight.value.dp - LocalTheme.current.shapes.betweenItemsSpace)
                                )
                            }
                        }
                    }
                    itemsIndexed(
                        items = if(networkItems.value == null) {
                            arrayOfNulls<NetworkItemIO?>(NETWORK_SHIMMER_ITEM_COUNT).toList()
                        } else networkItems.value.orEmpty(),
                        key = { _, item -> item?.publicId ?: Uuid.random().toString() }
                    ) { index, data ->
                        Column {
                            NetworkItemRow(
                                modifier = Modifier.animateItem(),
                                data = data,
                                isChecked = if(checkedItems.size > 0) checkedItems.contains(data?.publicId) else null,
                                onCheckChange = { isLongClick ->
                                    when {
                                        checkedItems.contains(data?.publicId) -> checkedItems.remove(data?.publicId)
                                        isLongClick || checkedItems.size > 0 -> {
                                            checkedItems.add(data?.publicId)
                                        }
                                        else -> navController?.navigate(
                                            NavigationNode.Conversation(userUid = data?.publicId)
                                        )
                                    }
                                }
                            )
                            if(index != networkItems.value?.size?.minus(1)) {
                                Divider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = LocalTheme.current.colors.disabledComponent,
                                    thickness = .3.dp
                                )
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace * 2))
                    }
                }
            }
        }
    }
}
