package ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_add_new
import augmy.composeapp.generated.resources.action_create_room
import augmy.composeapp.generated.resources.action_find_user
import augmy.composeapp.generated.resources.button_search
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.composeapp.generated.resources.screen_home
import augmy.composeapp.generated.resources.screen_home_initial_sync
import augmy.composeapp.generated.resources.screen_home_no_client_action
import augmy.composeapp.generated.resources.screen_home_no_client_title
import augmy.composeapp.generated.resources.screen_search_network
import augmy.interactive.shared.ext.onCtrlF
import augmy.interactive.shared.ext.onEscape
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.MinimalisticFilledIcon
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.utils.PersistentListData
import augmy.interactive.shared.utils.persistedLazyGridState
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.extractSnippetAroundHighlight
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
import data.io.matrix.room.FullConversationRoom
import data.io.matrix.room.RoomType
import data.io.user.NetworkItemIO
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.settings.ConversationDetailDialog
import ui.network.components.SocialItemActions
import ui.network.components.user_detail.UserDetailDialog
import ui.network.list.NETWORK_SHIMMER_ITEM_COUNT
import utils.SharedLogger
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
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact

    val conversationRooms = model.conversationRooms.collectAsLazyPagingItems()
    val uiMode = model.uiMode.collectAsState()

    val gridState = persistedLazyGridState(
        persistentData = model.persistentPositionData ?: PersistentListData(),
        onDispose = { lastInfo ->
            model.persistentPositionData = lastInfo
        }
    )
    val showTuner = rememberSaveable { mutableStateOf(false) }
    val selectedItem = rememberSaveable { mutableStateOf<String?>(null) }
    val showHomeActions = rememberSaveable { mutableStateOf(false) }
    val searchActivated = rememberSaveable { mutableStateOf(false) }
    val searchFieldState = remember { TextFieldState() }
    val focusRequester = remember { FocusRequester() }

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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OnBackHandler(enabled = selectedItem.value != null) {
        selectedItem.value = null
    }

    if (showTuner.value) {
        NetworkPreferencesLauncher(
            viewModel = model,
            onDismissRequest = {
                showTuner.value = false
            }
        )
    }

    RefreshableScreen(
        modifier = Modifier
            .focusable(true)
            .focusRequester(focusRequester)
            .onCtrlF {
                searchActivated.value = true
            }
            .onEscape {
                searchActivated.value = false
                showHomeActions.value = false
                showTuner.value = false
            },
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
            Crossfade(searchActivated.value) { activated ->
                ActionBarIcon(
                    text = if(isExpanded) stringResource(Res.string.screen_search_network) else null,
                    imageVector = if (activated) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                    onClick = {
                        searchActivated.value = !searchActivated.value
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        showHomeActions.value = false
                    })
                }
        ) {
            val categories = model.categories.collectAsState(initial = listOf())

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

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                visible = searchActivated.value
            ) {
                SearchField(
                    model = model,
                    searchFieldState = searchFieldState,
                    isCompact = isCompact
                )
            }

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
                            selectedItem = selectedItem,
                            searchFieldState = searchFieldState
                        )
                        HomeModel.UiMode.Circle -> SocialCircleContent(
                            modifier = Modifier.fillMaxSize(),
                            model = model
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .animateContentSize(),
            horizontalAlignment = Alignment.End
        ) {
            val rotation: Float by animateFloatAsState(
                targetValue = if (showHomeActions.value) 225f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )

            Icon(
                modifier = Modifier
                    .padding(end = 12.dp, bottom = 4.dp)
                    .scalingClickable {
                        showHomeActions.value = !showHomeActions.value
                    }
                    .size(48.dp)
                    .background(
                        color = LocalTheme.current.colors.appbarBackground,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(8.dp)
                    .rotate(rotation),
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.accessibility_add_new),
                tint = LocalTheme.current.colors.secondary
            )

            if (showHomeActions.value) {
                HomeActions(
                    isCompact,
                    onDismissRequest = {
                        showHomeActions.value = false
                    }
                )
            } else Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SearchField(
    model: HomeModel,
    searchFieldState: TextFieldState,
    isCompact: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(searchFieldState.text) {
        searchFieldState
        model.searchForMessages(searchFieldState.text)
    }

    CustomTextField(
        modifier = Modifier
            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
            .background(
                LocalTheme.current.colors.backgroundDark,
                shape = LocalTheme.current.shapes.rectangularActionShape
            )
            .padding(
                horizontal = 4.dp,
                vertical = 2.dp
            )
            .fillMaxWidth(if (isCompact) 1f else .8f),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        prefixIcon = Icons.Outlined.Search,
        isClearable = true,
        focusRequester = focusRequester,
        hint = stringResource(Res.string.button_search),
        state = searchFieldState,
        showBorders = false,
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = LocalTheme.current.shapes.rectangularActionShape
    )
}

@Composable
private fun HomeActions(
    isCompact: Boolean,
    onDismissRequest: () -> Unit
) {
    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .fillMaxWidth(if (isCompact) 1f else .5f)
            .background(
                color = LocalTheme.current.colors.backgroundDark,
                shape = if (isCompact) RectangleShape else RoundedCornerShape(
                    topStart = LocalTheme.current.shapes.rectangularActionRadius
                )
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .navigationBarsPadding()
    ) {
        RowAction(
            message = stringResource(Res.string.action_create_room),
            imageVector = Icons.Outlined.Tag,
            onClick = {
                onDismissRequest()
                navController?.navigate(NavigationNode.Conversation())
            }
        )
        RowAction(
            message = stringResource(Res.string.action_find_user),
            imageVector = Icons.Outlined.PersonSearch,
            onClick = {
                onDismissRequest()
                navController?.navigate(NavigationNode.SearchUser())
            }
        )
    }
}

@Composable
private fun RowAction(
    imageVector: ImageVector,
    message: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scalingClickable(scaleInto = .95f) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = LocalTheme.current.colors.secondary
        )
        Text(
            text = message,
            style = LocalTheme.current.styles.category.copy(
                color = LocalTheme.current.colors.secondary
            )
        )
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
    model: HomeModel,
    gridState: LazyGridState,
    searchFieldState: TextFieldState,
    selectedItem: MutableState<String?>
) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    val rooms = model.conversationRooms.collectAsLazyPagingItems()
    val customColors = model.customColors.collectAsState(initial = mapOf())
    val selectedUserId = model.selectedUserId.collectAsState()

    val isLoadingInitialPage = rooms.loadState.refresh is LoadState.Loading
            || (rooms.itemCount == 0 && !rooms.loadState.append.endOfPaginationReached)
    val isEmpty = rooms.itemCount == 0 && rooms.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact

    val selectedRoomId = remember { mutableStateOf<String?>(null) }


    selectedUserId.value?.let { userId ->
        UserDetailDialog(
            userId = userId,
            onDismissRequest = {
                model.selectUser(null)
            }
        )
    }
    selectedRoomId.value?.let { roomId ->
        ConversationDetailDialog(
            conversationId = roomId,
            onDismissRequest = {
                selectedRoomId.value = null
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
            if (isCompact || searchFieldState.text.isNotBlank()) 1 else 2
        ),
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {} // spacer
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                enter = expandVertically() + fadeIn(),
                visible = isEmpty
            ) {
                EmptyLayout(
                    title = stringResource(Res.string.network_list_empty_title),
                    action = stringResource(Res.string.network_list_empty_action),
                    onClick = {
                        navController?.navigate(NavigationNode.SearchUser())
                    }
                )
            }
        }
        items(
            count = if(rooms.itemCount == 0 && isLoadingInitialPage) {
                NETWORK_SHIMMER_ITEM_COUNT
            }else rooms.itemCount,
            key = { index -> rooms.getOrNull(index)?.id ?: Uuid.random().toString() }
        ) { index ->
            val room = rooms.getOrNull(index)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ConversationRoomItem(
                    modifier = Modifier.fillMaxWidth(
                        if (searchFieldState.text.isNotBlank() && !isCompact) .75f else 1f
                    ).animateItem(),
                    model = model,
                    room = room,
                    highlight = searchFieldState.text.toString().lowercase(),
                    selectedItem = selectedItem.value,
                    requestProximityChange = { proximity ->
                        val singleUser = if(room?.data?.summary?.isDirect == true) {
                            room.members.firstOrNull()
                        }else null

                        model.requestProximityChange(
                            conversationId = room?.id,
                            publicId = singleUser?.userId,
                            proximity = proximity,
                            onOperationDone = {
                                if(selectedItem.value == room?.id) {
                                    selectedItem.value = null
                                }
                                rooms.refresh()
                            }
                        )
                    },
                    customColors = customColors.value,
                    onTap = {
                        if (searchFieldState.text.isNotBlank()) {
                            navController?.navigate(
                                if (isCompact) {
                                    NavigationNode.ConversationSearch(
                                        conversationId = room?.id,
                                        searchQuery = searchFieldState.text.toString()
                                    )
                                } else NavigationNode.Conversation(
                                    conversationId = room?.id,
                                    searchQuery = searchFieldState.text.toString()
                                )
                            )
                        } else {
                            if(selectedItem.value == room?.id) {
                                selectedItem.value = null
                            }else navController?.navigate(
                                NavigationNode.Conversation(
                                    conversationId = room?.id,
                                    name = room?.name
                                )
                            )
                        }
                    },
                    onLongPress = {
                        selectedItem.value = room?.id
                    },
                    onAvatarClick = {
                        SharedLogger.logger.debug { "clicked room: $room" }
                        if(room?.data?.summary?.isDirect == true) {
                            model.selectUser(room)
                        }else {
                            selectedRoomId.value = room?.id
                        }
                    }
                ) {
                    if(index != rooms.itemCount - 1) {
                        HorizontalDivider(
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
}

@Composable
private fun ConversationRoomItem(
    modifier: Modifier = Modifier,
    model: HomeModel,
    selectedItem: String?,
    room: FullConversationRoom?,
    customColors: Map<NetworkProximityCategory, Color>,
    requestProximityChange: (proximity: Float) -> Unit,
    onAvatarClick: () -> Unit,
    highlight: String? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val navController = LocalNavController.current
    val collapsedRooms = model.collapsedRooms.collectAsState()
    val response = remember { mutableStateOf<BaseResponse<Any>?>(null) }

    LaunchedEffect(Unit) {
        model.requestResponse.collectLatest { responses ->
            response.value = responses[room?.id]
        }
    }

    LaunchedEffect(Unit) {
        if(room?.data?.summary?.isDirect == true) {
            model.requestOpenRooms()
        }
    }

    val indicatorColor = NetworkProximityCategory.entries.firstOrNull {
        it.range.contains(room?.data?.proximity ?: 1f)
    }.let {
        customColors[it] ?: it?.color
    }

    val itemModifier = Modifier
        .scalingClickable(
            enabled = room?.data?.type != RoomType.Invited,
            hoverEnabled = selectedItem != room?.id,
            scaleInto = .95f,
            onTap = { onTap() },
            onLongPress = { onLongPress() }
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

    Column(modifier = modifier.animateContentSize()) {
        val isSearched = !highlight.isNullOrBlank()

        NetworkItemRow(
            avatarSize = if (isSearched) 32.dp else 48.dp,
            modifier = itemModifier.then(
                if (isSearched) Modifier.alpha(.6f) else Modifier
            ),
            data = room?.toNetworkItem().let {
                it?.copy(lastMessage = if (isSearched) null else it.lastMessage)
            },
            isSelected = selectedItem == room?.id,
            indicatorColor = indicatorColor,
            onAvatarClick = onAvatarClick,
            content = {
                if (room?.data?.type == RoomType.Invited) {
                    NetworkRequestActions(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        key = room.id,
                        response = response.value,
                        onResponse = { accept ->
                            model.respondToInvitation(
                                roomId = room.id,
                                accept = accept
                            )
                        }
                    )
                }
            },
            actions = {
                if (room?.data?.type != RoomType.Invited) {
                    room?.toNetworkItem()?.let { networkItem ->
                        SocialItemActions(
                            key = room.id,
                            requestProximityChange = requestProximityChange,
                            newItem = networkItem
                        )
                    }
                }
            }
        )

        if (isSearched && !collapsedRooms.value.contains(room?.id)) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(.9f)
                    .animateContentSize()
            ) {
                room?.messages?.forEach { message ->
                    NetworkItemRow(
                        modifier = Modifier.scalingClickable(scaleInto = .95f) {
                            navController?.navigate(
                                NavigationNode.Conversation(conversationId = room.id, scrollTo = message.id)
                            )
                        },
                        highlightTitle = false,
                        data = NetworkItemIO(
                            userId = message.author?.userId,
                            displayName = message.author?.displayName,
                            avatarUrl = message.author?.avatarUrl,
                            lastMessage = if (highlight.isNotBlank()) {
                                extractSnippetAroundHighlight(message.data.content, highlight)
                            }else message.data.content
                        ),
                        highlight = highlight
                    )
                }
            }
        }
        content()
    }
}
