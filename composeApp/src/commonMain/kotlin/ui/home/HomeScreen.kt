package ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.FaceRetouchingOff
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.VoiceOverOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_cancel
import augmy.composeapp.generated.resources.accessibility_save
import augmy.composeapp.generated.resources.button_block
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.button_invite
import augmy.composeapp.generated.resources.button_mute
import augmy.composeapp.generated.resources.network_action_circle_move
import augmy.composeapp.generated.resources.network_dialog_message_block
import augmy.composeapp.generated.resources.network_dialog_message_mute
import augmy.composeapp.generated.resources.network_dialog_title_block
import augmy.composeapp.generated.resources.network_dialog_title_mute
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ext.horizontallyDraggable
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.OutlinedButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.theme.Colors
import base.utils.getOrNull
import components.EmptyLayout
import components.HorizontalScrollChoice
import components.OptionsLayoutAction
import components.ScrollChoice
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableScreen
import data.BlockedProximityValue
import data.NetworkProximityCategory
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import ui.network.add_new.NetworkAddNewLauncher
import ui.network.add_new.ProximityPicker
import ui.network.add_new.networkAddNewModule
import ui.network.list.NETWORK_SHIMMER_ITEM_COUNT
import ui.network.profile.UserProfileLauncher
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

    val conversationRooms = viewModel.conversationRooms.collectAsLazyPagingItems()
    val categories = viewModel.categories.collectAsState(initial = listOf())
    val customColors = viewModel.customColors.collectAsState(initial = mapOf())
    val isLoadingInitialPage = conversationRooms.loadState.refresh is LoadState.Loading
            || (conversationRooms.itemCount == 0 && !conversationRooms.loadState.append.endOfPaginationReached)
    val isEmpty = conversationRooms.itemCount == 0 && conversationRooms.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage

    val listState = rememberLazyGridState()
    val showTuner = rememberSaveable { mutableStateOf(false) }
    val isCompactView = rememberSaveable { mutableStateOf(true) }
    val selectedItem = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val showAddNewModal = rememberSaveable {
        mutableStateOf(false)
    }
    val selectedUser = remember {
        mutableStateOf<NetworkItemIO?>(null)
    }

    if(selectedUser.value != null) {
        UserProfileLauncher(
            userProfile = selectedUser.value,
            onDismissRequest = {
                selectedUser.value = null
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

    OnBackHandler(enabled = selectedItem.value != null) {
        selectedItem.value = null
    }

    RefreshableScreen(
        title = stringResource(Res.string.screen_home),
        navIconType = NavIconType.TUNE,
        onNavigationIconClick = {
            showTuner.value = true
        },
        onRefresh = {
            conversationRooms.refresh()
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
                Crossfade(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(1f),
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
                Crossfade(isCompactView.value) { isList ->
                    if(isList) {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        selectedItem.value = null
                                    })
                                }
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
                            item(span = { GridItemSpan(maxLineSpan) }) {}
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
                                count = if(conversationRooms.itemCount == 0 && isLoadingInitialPage) {
                                    NETWORK_SHIMMER_ITEM_COUNT
                                }else conversationRooms.itemCount,
                                key = { index -> conversationRooms.getOrNull(index)?.id ?: Uuid.random().toString() }
                            ) { index ->
                                conversationRooms.getOrNull(index).let { data ->
                                    Column(
                                        modifier = Modifier.animateItem()
                                    ) {
                                        NetworkItemRow(
                                            modifier = Modifier
                                                .scalingClickable(
                                                    hoverEnabled = selectedItem.value != data?.id,
                                                    scaleInto = .9f,
                                                    onTap = {
                                                        if(selectedItem.value == data?.id) {
                                                            selectedItem.value = null
                                                        }else navController?.navigate(
                                                            NavigationNode.Conversation(
                                                                conversationId = data?.id,
                                                                name = data?.summary?.alias
                                                            )
                                                        )
                                                    },
                                                    onLongPress = {
                                                        selectedItem.value = data?.id
                                                    }
                                                )
                                                .then(
                                                    (if(selectedItem.value != null && selectedItem.value == data?.id) {
                                                        Modifier
                                                            .background(
                                                                color = LocalTheme.current.colors.backgroundLight,
                                                                shape = LocalTheme.current.shapes.rectangularActionShape
                                                            )
                                                            .border(
                                                                width = 2.dp,
                                                                color = LocalTheme.current.colors.backgroundDark,
                                                                shape = LocalTheme.current.shapes.rectangularActionShape
                                                            )
                                                    }else Modifier)
                                                ),
                                            data = if(data == null) null else {
                                                NetworkItemIO(
                                                    name = data.summary?.alias,
                                                    tag = data.summary?.tag,
                                                    lastMessage = data.summary?.lastMessage?.body
                                                )
                                            },
                                            isSelected = selectedItem.value == data?.id,
                                            indicatorColor = NetworkProximityCategory.entries.firstOrNull {
                                                it.range.contains(data?.proximity ?: 1f)
                                            }.let {
                                                customColors.value[it] ?: it?.color
                                            },
                                            onAvatarClick = {
                                                if(data?.summary?.joinedMemberCount == 2) {
                                                    coroutineScope.launch(Dispatchers.Default) {
                                                        selectedUser.value = viewModel.networkItems.value?.find {
                                                            it.userMatrixId == data.summary.heroes?.firstOrNull()
                                                        }
                                                    }
                                                }
                                            },
                                            actions = {
                                                RoomActions(
                                                    data = data,
                                                    viewModel = viewModel,
                                                    refreshRequest = {
                                                        conversationRooms.refresh()
                                                    }
                                                )
                                            }
                                        )
                                        if(index != conversationRooms.itemCount - 1) {
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
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomActions(
    data: ConversationRoomIO?,
    viewModel: HomeViewModel,
    refreshRequest: () -> Unit
) {
    val showActionDialog = remember(data?.id) {
        mutableStateOf<OptionsLayoutAction?>(null)
    }
    val showMoveCircleDialog = remember(data?.id) {
        mutableStateOf(false)
    }
    val showInviteDialog = remember(data?.id) {
        mutableStateOf(false)
    }
    val selectedCategory = remember {
        mutableStateOf(NetworkProximityCategory.Public)
    }
    val actionsState = rememberScrollState()

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
                    conversationId = data?.id,
                    proximity = if(action == OptionsLayoutAction.Mute) {
                        NetworkProximityCategory.Public.range.start
                    }else BlockedProximityValue,
                    onOperationDone = {
                        refreshRequest()
                    }
                )
            },
            dismissButtonState = ButtonState(
                text = stringResource(Res.string.button_dismiss)
            ),
            onDismissRequest = {
                showActionDialog.value = null
            }
        )
    }

    if(showMoveCircleDialog.value) {
        loadKoinModules(networkAddNewModule)

        SimpleModalBottomSheet(
            onDismissRequest = {
                showMoveCircleDialog.value = false
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProximityPicker(
                viewModel = koinViewModel(),
                selectedCategory = selectedCategory.value,
                onSelectionChange = {
                    selectedCategory.value = it
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, alignment = Alignment.End)
            ) {
                OutlinedButton(
                    text = stringResource(Res.string.accessibility_cancel),
                    onClick = {
                        showMoveCircleDialog.value = false
                    },
                    activeColor = SharedColors.RED_ERROR_50
                )
                OutlinedButton(
                    text = stringResource(Res.string.accessibility_save),
                    onClick = {
                        viewModel.requestProximityChange(
                            conversationId = data?.id,
                            proximity = selectedCategory.value.range.start,
                            onOperationDone = {
                                refreshRequest()
                            }
                        )
                    },
                    activeColor = LocalTheme.current.colors.brandMain
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = LocalTheme.current.colors.backgroundDark,
                shape = RoundedCornerShape(
                    bottomEnd = LocalTheme.current.shapes.rectangularActionRadius,
                    bottomStart = LocalTheme.current.shapes.rectangularActionRadius,
                )
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .horizontalScroll(actionsState)
                .horizontallyDraggable(actionsState)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
            ScalingIcon(
                color = SharedColors.RED_ERROR.copy(.6f),
                imageVector = Icons.Outlined.FaceRetouchingOff,
                contentDescription = stringResource(Res.string.button_block),
                onClick = {
                    showActionDialog.value = OptionsLayoutAction.Block
                }
            )
            ScalingIcon(
                color = Colors.Coffee,
                imageVector = Icons.Outlined.VoiceOverOff,
                contentDescription = stringResource(Res.string.button_mute),
                onClick = {
                    showActionDialog.value = OptionsLayoutAction.Mute
                }
            )
            ScalingIcon(
                color = NetworkProximityCategory.Family.color,
                imageVector = Icons.Outlined.TrackChanges,
                contentDescription = stringResource(Res.string.network_action_circle_move),
                onClick = {
                    showMoveCircleDialog.value = true
                }
            )
            ScalingIcon(
                color = LocalTheme.current.colors.brandMain,
                imageVector = Icons.Outlined.GroupAdd,
                contentDescription = stringResource(Res.string.button_invite),
                onClick = {
                    showInviteDialog.value = true
                }
            )
            Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
        }
    }
}

@Composable
private fun ScalingIcon(
    color: Color,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier.background(
            color = color,
            shape = LocalTheme.current.shapes.componentShape
        )
    ) {
        Row(
            modifier = Modifier
                .scalingClickable {
                    onClick()
                }
                .background(
                    color = LocalTheme.current.colors.backgroundDark,
                    shape = LocalTheme.current.shapes.rectangularActionShape
                )
                .border(
                    width = 1.dp,
                    color = color,
                    shape = LocalTheme.current.shapes.rectangularActionShape
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp, end = 2.dp),
                text = contentDescription,
                style = LocalTheme.current.styles.regular
            )
            Icon(
                modifier = Modifier
                    .size(38.dp)
                    .padding(6.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = LocalTheme.current.colors.secondary
            )
        }
    }
}
