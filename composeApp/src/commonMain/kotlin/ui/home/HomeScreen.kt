package ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_invite
import augmy.composeapp.generated.resources.invite_message_explanation
import augmy.composeapp.generated.resources.invite_message_hint
import augmy.composeapp.generated.resources.invite_network_items_heading
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.getOrNull
import components.EmptyLayout
import components.HorizontalScrollChoice
import components.ScrollChoice
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableScreen
import data.NetworkProximityCategory
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.add_new.NetworkAddNewLauncher
import ui.network.components.SocialItemActions
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
                                conversationRooms.getOrNull(index).let { room ->
                                    ConversationRoomItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        viewModel = viewModel,
                                        room = room,
                                        selectedItem = selectedItem.value,
                                        requestProximityChange = { proximity ->
                                            viewModel.requestProximityChange(
                                                conversationId = room?.id,
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
                                                    name = room?.summary?.alias
                                                )
                                            )
                                        },
                                        onLongPress = {
                                            selectedItem.value = room?.id
                                        },
                                        onAvatarClick = {
                                            if(room?.summary?.joinedMemberCount == 2) {
                                                coroutineScope.launch(Dispatchers.Default) {
                                                    selectedUser.value = viewModel.networkItems.value?.find {
                                                        it.userMatrixId == room.summary.heroes?.firstOrNull()
                                                    }
                                                }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConversationRoomItem(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    selectedItem: String?,
    room: ConversationRoomIO?,
    customColors: Map<NetworkProximityCategory, Color>,
    requestProximityChange: (proximity: Float) -> Unit,
    onAvatarClick: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val showAddMembers = remember(room?.id) {
        mutableStateOf(false)
    }

    if(showAddMembers.value) {
        val networkItems = viewModel.networkItems.collectAsState()
        val checkedItems = remember {
            mutableStateListOf<String>()
        }

        SimpleModalBottomSheet(
            scrollEnabled = false,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            onDismissRequest = {
                showAddMembers.value = false
            }
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = stringResource(Res.string.invite_network_items_heading),
                style = LocalTheme.current.styles.title
            )
            LazyColumn(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .animateContentSize()
                    .fillMaxWidth()
            ) {
                stickyHeader {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = checkedItems.size > 0,
                        enter = slideInVertically (
                            initialOffsetY = { -it },
                            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
                        ),
                        exit = slideOutVertically (
                            targetOffsetY = { -it },
                            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
                        )
                    ) {
                        val messageState = remember(room?.id) {
                            val default = room?.summary?.invitationMessage ?: ""
                            TextFieldState(
                                initialText = default,
                                initialSelection = TextRange(default.length)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomTextField(
                                modifier = Modifier
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 6.dp
                                    )
                                    .background(
                                        LocalTheme.current.colors.backgroundLight,
                                        shape = LocalTheme.current.shapes.componentShape
                                    )
                                    .requiredHeight(44.dp)
                                    .weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Send
                                ),
                                suggestText = stringResource(Res.string.invite_message_explanation),
                                hint = stringResource(Res.string.invite_message_hint),
                                state = messageState,
                                onKeyboardAction = {
                                    viewModel.inviteToConversation(
                                        conversationId = room?.id,
                                        userPublicIds = checkedItems.toList(),
                                        message = messageState.text.toString()
                                    )
                                    showAddMembers.value = false
                                },
                                lineLimits = TextFieldLineLimits.SingleLine,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                            BrandHeaderButton(
                                text = stringResource(Res.string.button_invite)
                            ) {
                                viewModel.inviteToConversation(
                                    conversationId = room?.id,
                                    userPublicIds = checkedItems.toList(),
                                    message = messageState.text.toString()
                                )
                                showAddMembers.value = false
                            }
                        }
                    }
                }
                items(
                    items = networkItems.value.orEmpty(),
                    key = { it.publicId }
                ) { data ->
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetworkItemRow(
                            modifier = Modifier
                                .scalingClickable(
                                    scaleInto = .95f,
                                    onTap = {
                                        if(checkedItems.contains(data.publicId)) {
                                            checkedItems.remove(data.publicId)
                                        }else {
                                            checkedItems.add(data.publicId)
                                        }
                                    }
                                )
                                .weight(1f),
                            data = data
                        )
                        Checkbox(
                            checked = checkedItems.contains(data.publicId),
                            onCheckedChange = {
                                if(checkedItems.contains(data.publicId)) {
                                    checkedItems.remove(data.publicId)
                                }else {
                                    checkedItems.add(data.publicId)
                                }
                            },
                            colors = LocalTheme.current.styles.checkBoxColorsDefault
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
    }

    Column(modifier = modifier) {
        NetworkItemRow(
            modifier = Modifier
                .scalingClickable(
                    hoverEnabled = selectedItem != room?.id,
                    scaleInto = .9f,
                    onTap = {
                        onTap()
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
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
                ),
            data = if(room == null) null else {
                NetworkItemIO(
                    name = room.summary?.alias,
                    tag = room.summary?.tag,
                    photoUrl = room.summary?.avatarUrl,
                    lastMessage = room.summary?.lastMessage?.body
                )
            },
            isSelected = selectedItem == room?.id,
            indicatorColor = NetworkProximityCategory.entries.firstOrNull {
                it.range.contains(room?.proximity ?: 1f)
            }.let {
                customColors[it] ?: it?.color
            },
            onAvatarClick = onAvatarClick,
            actions = {
                SocialItemActions(
                    key = room?.id,
                    requestProximityChange = requestProximityChange,
                    onInvite = {
                        showAddMembers.value = true
                    },
                    newItem = NetworkItemIO(
                        name = room?.summary?.alias,
                        tag = room?.summary?.tag,
                        photoUrl = room?.summary?.avatarUrl,
                        publicId = room?.id ?: "-",
                        proximity = room?.proximity
                    )
                )
            }
        )
        content()
    }
}
