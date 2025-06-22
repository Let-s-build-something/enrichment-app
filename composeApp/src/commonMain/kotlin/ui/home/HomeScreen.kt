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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.invite_conversation_heading
import augmy.composeapp.generated.resources.invite_network_items_heading
import augmy.composeapp.generated.resources.invite_new_item_conversation
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_home_initial_sync
import augmy.composeapp.generated.resources.screen_home_no_client_action
import augmy.composeapp.generated.resources.screen_home_no_client_title
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.utils.PersistentListData
import augmy.interactive.shared.utils.persistedLazyGridState
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.getOrNull
import components.EmptyLayout
import components.HorizontalScrollChoice
import components.ScrollChoice
import components.network.NetworkItemRow
import components.network.NetworkRequestActions
import components.pull_refresh.RefreshableScreen
import data.NetworkProximityCategory
import data.io.base.AppPingType
import data.io.base.BaseResponse
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.RoomType
import data.io.user.NetworkItemIO
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.add_new.NetworkAddNewLauncher
import ui.network.components.AddToLauncher
import ui.network.components.SocialItemActions
import ui.network.components.user_detail.UserDetailDialog
import ui.network.list.NETWORK_SHIMMER_ITEM_COUNT
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Screen for the home page
 */
@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(model: HomeModel = koinViewModel()) {
    val coroutineScope = rememberCoroutineScope()
    val navController = LocalNavController.current

    val conversationRooms = model.conversationRooms.collectAsLazyPagingItems()
    val categories = model.categories.collectAsState(initial = listOf())
    val uiMode = model.uiMode.collectAsState()

    val gridState = persistedLazyGridState(
        persistentData = model.persistentPositionData ?: PersistentListData(),
        onDispose = { lastInfo ->
            model.persistentPositionData = lastInfo
        }
    )
    val showTuner = rememberSaveable { mutableStateOf(false) }
    val selectedItem = rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if(model.persistentPositionData != null) {
            conversationRooms.refresh()
        }
    }

    LaunchedEffect(Unit) {
        model.pingStream.collectLatest { stream ->
            stream.forEach {
                if(it.type == AppPingType.ConversationDashboard) {
                    conversationRooms.refresh()
                }
            }
        }
    }

    if(showTuner.value) {
        NetworkPreferencesLauncher(
            viewModel = model,
            onDismissRequest = {
                showTuner.value = false
            }
        )
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
                gridState.animateScrollToItem(0)
            }
        },
        showDefaultActions = true,
        viewModel = model,
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
                    if (isSelected) {
                        newList.add(item)
                    } else {
                        newList.removeAll { it == item }
                    }
                    if (newList.isEmpty()) {
                        newList.add(if(item == NetworkProximityCategory.Family) {
                            NetworkProximityCategory.Peers
                        }else NetworkProximityCategory.Family)
                    }
                    model.filterNetworkItems(filter = newList)
                },
                selectedItems = categories.value
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                if (uiMode.value.isFinished) {
                    Crossfade(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .zIndex(1f),
                        targetState = uiMode.value == HomeModel.UiMode.List
                    ) { isList ->
                        MinimalisticFilledIcon(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .zIndex(1f),
                            imageVector = if (isList) Icons.Outlined.TrackChanges else Icons.AutoMirrored.Outlined.List,
                            onTap = {
                                model.swapUiMode(!isList)
                            }
                        )
                    }
                }
                Crossfade(targetState = uiMode.value) { mode ->
                    when (mode) {
                        HomeModel.UiMode.List -> ListContent(
                            model = model,
                            gridState = gridState,
                            selectedItem = selectedItem
                        )
                        HomeModel.UiMode.Circle -> SocialCircleContent(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = model
                        )
                        HomeModel.UiMode.Loading -> EmptyLayout(
                            modifier = Modifier.fillMaxSize(),
                            title = stringResource(Res.string.screen_home_initial_sync),
                            animReverseOnRepeat = false,
                            animSpec = {
                                LottieCompositionSpec.DotLottie(Res.readBytes("files/loading_envelope.lottie"))
                            }
                        )
                        HomeModel.UiMode.NoClient -> EmptyLayout(
                            title = stringResource(Res.string.screen_home_no_client_title),
                            action = stringResource(Res.string.screen_home_no_client_action),
                            onClick = {
                                navController?.navigate(NavigationNode.Login())
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
    model: HomeModel,
    gridState: LazyGridState,
    selectedItem: MutableState<String?>
) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    val conversationRooms = model.conversationRooms.collectAsLazyPagingItems()
    val customColors = model.customColors.collectAsState(initial = mapOf())
    val isLoadingInitialPage = conversationRooms.loadState.refresh is LoadState.Loading
            || (conversationRooms.itemCount == 0 && !conversationRooms.loadState.append.endOfPaginationReached)
    val isEmpty = conversationRooms.itemCount == 0 && conversationRooms.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage

    val selectedUser = remember {
        mutableStateOf<String?>(null)
    }
    val showAddNewModal = rememberSaveable {
        mutableStateOf(false)
    }

    if(showAddNewModal.value) {
        NetworkAddNewLauncher(onDismissRequest = {
            showAddNewModal.value = false
        })
    }

    if(selectedUser.value != null) {
        UserDetailDialog(
            userId = selectedUser.value,
            onDismissRequest = {
                selectedUser.value = null
            }
        )
    }

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
                        gridState.scrollBy(-delta)
                    }
                }
            )
            .fillMaxSize(),
        columns = GridCells.Fixed(
            if(LocalDeviceType.current == WindowWidthSizeClass.Compact) 1 else 2
        ),
        state = gridState,
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
            val room = conversationRooms.getOrNull(index)

            ConversationRoomItem(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth(),
                model = model,
                room = room,
                selectedItem = selectedItem.value,
                requestProximityChange = { proximity ->
                    val singleUser = if(room?.summary?.isDirect == true) {
                        room.summary.members?.firstOrNull()
                    }else null

                    model.requestProximityChange(
                        conversationId = room?.id,
                        publicId = singleUser?.userId,
                        proximity = proximity,
                        onOperationDone = {
                            if(selectedItem.value == room?.id) {
                                selectedItem.value = null
                            }
                            conversationRooms.refresh()
                        }
                    )
                },
                customColors = customColors.value,
                onTap = {
                    if(selectedItem.value == room?.id) {
                        selectedItem.value = null
                    }else navController?.navigate(
                        NavigationNode.Conversation(
                            conversationId = room?.id,
                            name = room?.summary?.roomName
                        )
                    )
                },
                onLongPress = {
                    selectedItem.value = room?.id
                },
                onAvatarClick = {
                    if(room?.summary?.isDirect == true) {
                        coroutineScope.launch(Dispatchers.Default) {
                            selectedUser.value = room.summary.heroes?.firstOrNull()?.full ?: room.summary.members?.firstOrNull()?.id
                        }
                    }else {
                        // TODO room settings window dialog
                    }
                }
            ) {
                if(index != conversationRooms.itemCount - 1) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        color = LocalTheme.current.colors.disabledComponent,
                        thickness = .3.dp
                    )
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
}

@Composable
private fun ConversationRoomItem(
    modifier: Modifier = Modifier,
    model: HomeModel,
    selectedItem: String?,
    room: ConversationRoomIO?,
    customColors: Map<NetworkProximityCategory, Color>,
    requestProximityChange: (proximity: Float) -> Unit,
    onAvatarClick: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val response = remember {
        mutableStateOf<BaseResponse<Any>?>(null)
    }

    val navController = LocalNavController.current
    val showAddMembers = remember(room?.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        model.requestResponse.collectLatest { responses ->
            response.value = responses[room?.id]
        }
    }

    LaunchedEffect(Unit) {
        if(room?.summary?.isDirect == true) {
            model.requestOpenRooms()
        }
    }

    if(showAddMembers.value) {
        val isLoading = model.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            model.invitationResponse.collectLatest {
                if(!it?.conversationId.isNullOrBlank()) {
                    navController?.navigate(
                        NavigationNode.Conversation(
                            conversationId = it.conversationId,
                            name = it.alias
                        )
                    )
                }
            }
        }

        if(room?.summary?.isDirect == true) {
            val conversations = model.openConversations.collectAsState()

            AddToLauncher(
                key = room.id,
                multiSelect = false,
                isLoading = isLoading.value,
                heading = stringResource(
                    Res.string.invite_conversation_heading,
                    room.summary.members?.firstOrNull()?.content?.displayName ?: "?"
                ),
                newItemHint = stringResource(Res.string.invite_new_item_conversation),
                items = conversations.value,
                mapToNetworkItem = { it.toNetworkItem() },
                onInvite = { checkedItems, message, newName ->
                    model.inviteToConversation(
                        conversationId = if(newName != null) null else checkedItems.firstOrNull()?.id,
                        userPublicIds = room.summary.members?.firstOrNull()?.userId?.let { listOf(it) },
                        message = message,
                        newName = newName
                    )
                },
                onDismissRequest = {
                    showAddMembers.value = false
                }
            )
        }else {
            val networkItems = model.networkItems.collectAsState(null)

            AddToLauncher(
                key = room?.id,
                defaultMessage = room?.summary?.invitationMessage,
                multiSelect = true,
                isLoading = isLoading.value,
                heading = stringResource(Res.string.invite_network_items_heading),
                items = networkItems.value,
                mapToNetworkItem = { it },
                onInvite = { checkedItems, message, _ ->
                    model.inviteToConversation(
                        conversationId = room?.id,
                        userPublicIds = checkedItems.mapNotNull { it.userPublicId },
                        message = message
                    )
                },
                onDismissRequest = {
                    showAddMembers.value = false
                }
            )
        }
    }

    val indicatorColor = NetworkProximityCategory.entries.firstOrNull {
        it.range.contains(room?.proximity ?: 1f)
    }.let {
        customColors[it] ?: it?.color
    }
    val itemModifier = Modifier
        .scalingClickable(
            enabled = room?.type != RoomType.Invited,
            hoverEnabled = selectedItem != room?.id,
            scaleInto = .9f,
            onTap = {
                onTap()
            },
            onLongPress = {
                onLongPress()
            }
        )
        .fillMaxWidth()
        .then(
            (if(selectedItem != null && selectedItem == room?.id) {
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
        )

    Column(modifier = modifier) {
        Crossfade(room?.type) { roomType ->
            when(roomType) {
                RoomType.Invited -> {
                    NetworkItemRow(
                        modifier = itemModifier,
                        data = room?.toNetworkItem(),
                        indicatorColor = indicatorColor,
                        onAvatarClick = onAvatarClick,
                        content = {
                            NetworkRequestActions(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                key = room?.id,
                                response = response.value,
                                onResponse = { accept ->
                                    model.respondToInvitation(
                                        roomId = room?.id,
                                        accept = accept
                                    )
                                }
                            )
                        }
                    )
                }
                else -> {
                    NetworkItemRow(
                        modifier = itemModifier,
                        data = room?.toNetworkItem(),
                        isSelected = selectedItem == room?.id,
                        indicatorColor = indicatorColor,
                        onAvatarClick = onAvatarClick,
                        actions = {
                            SocialItemActions(
                                key = room?.id,
                                requestProximityChange = requestProximityChange,
                                onInvite = {
                                    showAddMembers.value = true
                                },
                                newItem = NetworkItemIO(
                                    displayName = room?.summary?.roomName,
                                    avatar = room?.summary?.roomAvatar,
                                    publicId = room?.id ?: "-",
                                    proximity = room?.proximity
                                )
                            )
                        }
                    )
                }
            }
        }
        content()
    }
}
