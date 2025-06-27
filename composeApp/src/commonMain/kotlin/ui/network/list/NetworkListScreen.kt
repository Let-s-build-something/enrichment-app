package ui.network.list

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.invite_conversation_heading
import augmy.composeapp.generated.resources.invite_new_item_conversation
import augmy.composeapp.generated.resources.network_list_empty_action
import augmy.composeapp.generated.resources.network_list_empty_title
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import base.utils.getOrNull
import collectResult
import components.EmptyLayout
import components.network.NetworkItemRow
import components.pull_refresh.RefreshableContent
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.NetworkProximityCategory
import data.io.base.AppPingType
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.network.RefreshHandler
import ui.network.components.AddToLauncher
import ui.network.components.SocialItemActions
import ui.network.components.user_detail.UserDetailDialog
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Screen containing current user's network and offers its management */
@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NetworkListContent(
    openAddNewModal: () -> Unit,
    refreshHandler: RefreshHandler,
    viewModel: NetworkListModel = koinViewModel()
) {
    val networkItems = viewModel.networkItems.collectAsLazyPagingItems()
    val isRefreshing = viewModel.isRefreshing.collectAsState()
    val customColors = viewModel.customColors.collectAsState(initial = mapOf())

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val navController = LocalNavController.current
    val isLoadingInitialPage = networkItems.loadState.refresh is LoadState.Loading

    val selectedItem = remember { mutableStateOf<String?>(null) }
    val selectedUser = remember {
        mutableStateOf<NetworkItemIO?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.pingStream.collectLatest { stream ->
            stream.forEach {
                if(it.type == AppPingType.NetworkDashboard) {
                    networkItems.refresh()
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

    if(selectedUser.value != null) {
        UserDetailDialog(networkItem = selectedUser.value) {
            selectedUser.value = null
        }
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
}



@Composable
private fun NetworkItem(
    modifier: Modifier = Modifier,
    viewModel: NetworkListModel,
    selectedItem: String?,
    data: NetworkItemIO?,
    customColors: Map<NetworkProximityCategory, Color>,
    requestProximityChange: (proximity: Float) -> Unit,
    onAvatarClick: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    content: @Composable () -> Unit
) {
    val navController = LocalNavController.current
    val showAddMembers = remember(data?.publicId) {
        mutableStateOf(false)
    }

    // preload the conversations
    LaunchedEffect(Unit) {
        viewModel.requestOpenConversations()
    }

    if(showAddMembers.value) {
        val isLoading = viewModel.isLoading.collectAsState()
        val conversations = viewModel.openConversations.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.response.collectLatest {
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

        AddToLauncher(
            key = data?.publicId,
            items = conversations.value,
            heading = stringResource(
                Res.string.invite_conversation_heading,
                data?.displayName ?: ""
            ),
            newItemHint = stringResource(Res.string.invite_new_item_conversation),
            multiSelect = false,
            isLoading = isLoading.value,
            onInvite = { checkedItems, message, newName ->
                viewModel.inviteToConversation(
                    conversationId = checkedItems.firstOrNull()?.id,
                    userPublicIds = data?.userPublicId?.let { listOf(it) },
                    message = message,
                    newName = newName
                )
            },
            mapToNetworkItem = { it.toNetworkItem() },
            onDismissRequest = {
                showAddMembers.value = false
            }
        )
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
                .fillMaxWidth()
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