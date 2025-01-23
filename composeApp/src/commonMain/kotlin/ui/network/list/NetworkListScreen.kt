package ui.network.list

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_invite
import augmy.composeapp.generated.resources.invite_conversation_heading
import augmy.composeapp.generated.resources.invite_message_explanation
import augmy.composeapp.generated.resources.invite_message_hint
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.DEFAULT_ANIMATION_LENGTH_SHORT
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationArguments
import base.utils.getOrNull
import collectResult
import components.EmptyLayout
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableContent
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.NetworkProximityCategory
import data.io.social.network.conversation.matrix.ConversationRoomIO
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.RefreshHandler
import ui.network.components.SocialItemActions
import ui.network.profile.UserProfileLauncher
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Screen containing current user's network and offers its management */
@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NetworkListContent(
    openAddNewModal: () -> Unit,
    refreshHandler: RefreshHandler,
    viewModel: NetworkListViewModel = koinViewModel()
) {
    val networkItems = viewModel.networkItems.collectAsLazyPagingItems()
    val isRefreshing = viewModel.isRefreshing.collectAsState()
    val customColors = viewModel.customColors.collectAsState(initial = mapOf())

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val navController = LocalNavController.current
    val isLoadingInitialPage = networkItems.loadState.refresh is LoadState.Loading

    val checkedItems = remember { mutableStateListOf<String?>() }
    val selectedItem = remember { mutableStateOf<String?>(null) }
    val selectedUser = remember {
        mutableStateOf<NetworkItemIO?>(null)
    }

    navController?.collectResult(
        key = NavigationArguments.NETWORK_NEW_SUCCESS,
        defaultValue = false,
        listener = { isSuccess ->
            if(isSuccess) networkItems.refresh()
        }
    )

    if(selectedUser.value != null) {
        UserProfileLauncher(
            userProfile = selectedUser.value,
            onDismissRequest = {
                selectedUser.value = null
            }
        )
    }

    LaunchedEffect(Unit) {
        refreshHandler.addListener {
            viewModel.requestData(isSpecial = true, isPullRefresh = true)
            networkItems.refresh()
        }
    }

    RefreshableContent(
        onRefresh = {
            viewModel.requestData(isSpecial = true, isPullRefresh = true)
            networkItems.refresh()
        },
        isRefreshing = isRefreshing.value
    ) {
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
            item(span = { GridItemSpan(maxLineSpan) }) {
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
                key = { index -> networkItems.getOrNull(index)?.userPublicId ?: Uuid.random().toString() }
            ) { index ->
                networkItems.getOrNull(index).let { data ->
                    NetworkItem(
                        modifier = Modifier.animateItem(),
                        viewModel = viewModel,
                        selectedItem = selectedItem.value,
                        data = data,
                        customColors = customColors.value,
                        requestProximityChange = { proximity ->
                            viewModel.requestProximityChange(
                                publicId = data?.publicId,
                                proximity = proximity,
                                onOperationDone = {
                                    if(selectedItem.value == data?.userPublicId) {
                                        selectedItem.value = null
                                    }
                                    networkItems.refresh()
                                }
                            )
                        },
                        onAvatarClick = {
                            coroutineScope.launch(Dispatchers.Default) {
                                selectedUser.value = data
                            }
                        },
                        onTap = {
                            selectedItem.value = if(selectedItem.value != data?.userPublicId) {
                                data?.userPublicId
                            }else null
                        },
                        onLongPress = {
                            selectedItem.value = data?.userPublicId
                        }
                    ) {
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NetworkItem(
    modifier: Modifier = Modifier,
    viewModel: NetworkListViewModel,
    selectedItem: String?,
    data: NetworkItemIO?,
    customColors: Map<NetworkProximityCategory, Color>,
    requestProximityChange: (proximity: Float) -> Unit,
    onAvatarClick: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val showAddMembers = remember(data?.publicId) {
        mutableStateOf(false)
    }

    // preload the conversations
    LaunchedEffect(Unit) {
        viewModel.requestConversations()
    }

    if(showAddMembers.value) {
        val conversations = viewModel.conversations.collectAsState()
        val checkedItem = remember {
            mutableStateOf<ConversationRoomIO?>(null)
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
                text = stringResource(
                    Res.string.invite_conversation_heading,
                    data?.name ?: ""
                ),
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
                        visible = checkedItem.value != null,
                        enter = slideInVertically (
                            initialOffsetY = { -it },
                            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
                        ),
                        exit = slideOutVertically (
                            targetOffsetY = { -it },
                            animationSpec = tween(DEFAULT_ANIMATION_LENGTH_SHORT)
                        )
                    ) {

                        val messageState = remember(data?.publicId) {
                            val default = checkedItem.value?.summary?.invitationMessage ?: ""
                            TextFieldState(
                                initialText = default,
                                initialSelection = TextRange(default.length)
                            )
                        }

                        Row(
                            modifier = Modifier.animateItem(),
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
                                        conversationId = checkedItem.value?.id,
                                        userPublicId = data?.publicId,
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
                                    conversationId = checkedItem.value?.id,
                                    userPublicId = data?.publicId,
                                    message = messageState.text.toString()
                                )
                                showAddMembers.value = false
                            }
                        }
                    }
                }
                items(
                    items = conversations.value.orEmpty(),
                    key = { it.id }
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
                                        checkedItem.value = if(checkedItem.value?.id != data.id) data else null
                                    }
                                )
                                .weight(1f),
                            data = NetworkItemIO(
                                name = data.summary?.alias,
                                proximity = data.summary?.proximity,
                                publicId = data.id,
                                photoUrl = data.summary?.avatarUrl
                            )
                        )
                        Checkbox(
                            checked = checkedItem.value?.id == data.id,
                            onCheckedChange = {
                                checkedItem.value = if(checkedItem.value?.id != data.id) data else null
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
                    hoverEnabled = selectedItem != data?.userPublicId,
                    scaleInto = .9f,
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
                .then(
                    (if(selectedItem != null && selectedItem == data?.userPublicId) {
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
                .fillMaxWidth(),
            data = data,
            indicatorColor = NetworkProximityCategory.entries.firstOrNull {
                it.range.contains(data?.proximity ?: 1f)
            }.let {
                customColors[it] ?: it?.color
            },
            onAvatarClick = onAvatarClick,
            isSelected = selectedItem == data?.userPublicId,
            actions = {
                if(data != null) {
                    SocialItemActions(
                        key = data.userPublicId,
                        requestProximityChange = requestProximityChange,
                        onInvite = {
                            showAddMembers.value = true
                        },
                        newItem = data
                    )
                }
            }
            /*onCheckChange = { isLongClick ->
                when {
                    checkedItems.contains(data?.userPublicId) -> checkedItems.remove(data?.userPublicId)
                    isLongClick || checkedItems.size > 0 -> {
                        selectedItem.value = null
                        checkedItems.add(data?.userPublicId)
                    }
                    else -> {
                        selectedItem.value = if(selectedItem.value == data?.userPublicId) null else data?.userPublicId
                    }
                }
            }*/
        )
        content()
    }
}

/** Number of network items within one screen to be shimmered */
const val NETWORK_SHIMMER_ITEM_COUNT = 20