package ui.conversation

import NavigationHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.rememberNavController
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_add_new
import augmy.composeapp.generated.resources.action_remove
import augmy.composeapp.generated.resources.action_settings
import augmy.composeapp.generated.resources.conversation_create_helper
import augmy.composeapp.generated.resources.conversation_create_search_hint
import augmy.composeapp.generated.resources.conversation_create_title
import augmy.composeapp.generated.resources.conversation_mode_default
import augmy.composeapp.generated.resources.conversation_mode_experimental
import augmy.composeapp.generated.resources.conversation_new_room
import augmy.composeapp.generated.resources.conversation_selected_users
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalLinkHandler
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.components.input.DELAY_BETWEEN_TYPING_SHORT
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import base.navigation.NestedNavigationBar
import collectResult
import components.UserProfileImage
import components.network.NetworkItemRow
import components.pull_refresh.LocalRefreshCallback
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.io.base.AppPingType
import data.io.matrix.room.event.ConversationRoomMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.prototype.PrototypeConversation
import utils.SharedLogger

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24

/** Screen displaying a conversation */
@Composable
fun ConversationScreen(
    conversationId: String? = null,
    userId: String? = null,
    name: String? = null
) {
    loadKoinModules(conversationModule)
    val model: ConversationModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId, userId, true)
        }
    )
    val navController = LocalNavController.current
    val deviceType = LocalDeviceType.current

    val messages = model.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = model.conversation.collectAsState(initial = null)
    val uiMode = model.uiMode.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val reactingToMessageId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val showSettings = rememberSaveable {
        mutableStateOf(false)
    }
    val modeSwitchState = rememberMultiChoiceState(
        items = mutableListOf(
            stringResource(Res.string.conversation_mode_default),
            stringResource(Res.string.conversation_mode_experimental)
        )
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if(model.persistentPositionData != null) {
            messages.refresh()
        }
    }

    LaunchedEffect(Unit) {
        if(conversationId != null) model.consumePing(conversationId)
    }

    LaunchedEffect(Unit) {
        model.pingStream.collectLatest { stream ->
            stream.forEach {
                SharedLogger.logger.debug { "Ping received: $it" }
                if(conversationId != null
                    && it.type == AppPingType.Conversation
                    && it.identifier == conversationId
                ) {
                    messages.refresh()
                    model.consumePing(conversationId)
                }
            }
        }
    }

    navController?.collectResult(
        key = NavigationArguments.CONVERSATION_NEW_MESSAGE,
        defaultValue = false,
        listener = { newMessages ->
            if(newMessages) {
                coroutineScope.launch(Dispatchers.Main) {
                    delay(500)
                    messages.refresh()
                }
            }
        }
    )

    OnBackHandler(enabled = reactingToMessageId.value != null) {
        reactingToMessageId.value = null
    }

    CompositionLocalProvider(
        LocalRefreshCallback provides {
            model.requestData(isSpecial = true, isPullRefresh = true)
        }
    ) {
        BrandBaseScreen(
            navIconType = NavIconType.BACK,
            headerPrefix = {
                AnimatedVisibility(conversationDetail.value != null) {
                    Row {
                        UserProfileImage(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(32.dp),
                            media = conversationDetail.value?.avatar,
                            tag = conversationDetail.value?.data?.tag,
                            animate = true,
                            name = conversationDetail.value?.name
                        )
                        Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                    }
                }
            },
            actionIcons = { isExpanded ->
                ActionBarIcon(
                    text = if(isExpanded && LocalDeviceType.current != WindowWidthSizeClass.Compact) {
                        stringResource(Res.string.action_settings)
                    } else null,
                    imageVector = Icons.Outlined.MoreVert,
                    onClick = {
                        if(deviceType == WindowWidthSizeClass.Compact) {
                            navController?.navigate(NavigationNode.ConversationSettings(conversationId))
                        }else showSettings.value = !showSettings.value
                    }
                )
            },
            clearFocus = false,
            title = conversationDetail.value?.name ?: name ?: stringResource(Res.string.conversation_new_room).takeIf {
                conversationId == null
            }
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.Bottom
            ) {
                Crossfade(
                    modifier = Modifier.weight(1f),
                    targetState = modeSwitchState.selectedTabIndex.value
                ) { selectedIndex ->
                    if(selectedIndex == 1) {
                        PrototypeConversation(conversationId = conversationId)
                    }else {
                        val scopeItems: LazyListScope.() -> Unit = if (uiMode.value == ConversationModel.UiMode.CreateRoomNoMembers) {
                            val recommendedUsersToInvite = model.recommendedUsersToInvite.collectAsState()
                            val membersToInvite = model.membersToInvite.collectAsState()
                            val searchState = remember(model) { TextFieldState() }
                            val focusRequester = remember(model) { FocusRequester() }
                            val cancellableScope = rememberCoroutineScope()

                            LaunchedEffect(Unit) {
                                model.recommendUsersToInvite()
                            }

                            LaunchedEffect(searchState.text) {
                                cancellableScope.coroutineContext.cancelChildren()
                                cancellableScope.launch {
                                    delay(DELAY_BETWEEN_TYPING_SHORT)
                                    model.recommendUsersToInvite(query = searchState.text)
                                }
                            };

                            { scope: LazyListScope ->
                                scope.createRoomNoMembers(
                                    model = model,
                                    recommendedUserToInvite = recommendedUsersToInvite,
                                    membersToInvite = membersToInvite,
                                    searchState = searchState,
                                    focusRequester = focusRequester
                                )
                            }
                        }else { scope: LazyListScope ->
                            scope.item(key = "topPadding") {
                                Spacer(Modifier.height(42.dp))
                            }
                        }

                        ConversationComponent(
                            listModifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            lazyScope = {
                                scopeItems(this)
                            },
                            messages = messages,
                            conversationId = conversationId,
                            shimmerItemCount = MESSAGES_SHIMMER_ITEM_COUNT,
                            model = model
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .animateContentSize(alignment = Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(if(showSettings.value) LocalScreenSize.current.width.times(.4f).dp else 0.dp)
                ) {
                    if(showSettings.value) {
                        val nestedNavController = rememberNavController()

                        Column {
                            if(BuildKonfig.isDevelopment) {
                                MultiChoiceSwitch(
                                    modifier = Modifier.fillMaxWidth(),
                                    state = modeSwitchState
                                )
                            }
                            NestedNavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                navController = nestedNavController
                            )
                            NavigationHost(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = LocalTheme.current.colors.backgroundLight),
                                startDestination = NavigationNode.ConversationSettings(conversationId),
                                navController = nestedNavController,
                                enterTransition = {
                                    slideInHorizontally { -it * 2 }
                                },
                                exitTransition = {
                                    slideOutHorizontally { -it * 2 }
                                }
                            )
                        }

                        nestedNavController.collectResult(
                            key = NavigationArguments.CONVERSATION_LEFT,
                            defaultValue = false,
                            listener = { left ->
                                if(left) navController?.navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.createRoomNoMembers(
    model: ConversationModel,
    focusRequester: FocusRequester,
    recommendedUserToInvite: State<List<ConversationRoomMember>>,
    membersToInvite: State<MutableSet<ConversationRoomMember>>,
    searchState: TextFieldState
) {
    item(key = "helper") {
        Text(
            modifier = Modifier.padding(top = 32.dp, bottom = 4.dp, start = 8.dp),
            text = stringResource(Res.string.conversation_create_helper),
            style = LocalTheme.current.styles.regular
        )
    }
    items(
        items = recommendedUserToInvite.value,
        key = { "recommended_${it.id}" }
    ) { member ->
        val linkHandler = LocalLinkHandler.current

        NetworkItemRow(
            modifier = Modifier
                .animateItem()
                .wrapContentWidth()
                .padding(start = 8.dp)
                .scalingClickable(scaleInto = .95f) {
                    model.selectInvitedMember(member)
                    model.recommendUsersToInvite(query = searchState.text)
                },
            highlight = searchState.text.toString(),
            data = member.toNetworkItem(),
            onAvatarClick = {
                linkHandler?.invoke("/#/${member.userId}")
            },
        ) {
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.accessibility_add_new),
                tint = LocalTheme.current.colors.secondary
            )
        }
    }
    stickyHeader(key = "searchHeader") {
        CustomTextField(
            modifier = Modifier
                .zIndex(1f)
                .background(
                    color = LocalTheme.current.colors.backgroundDark,
                    shape = LocalTheme.current.shapes.rectangularActionShape
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .fillMaxWidth(if (LocalDeviceType.current == WindowWidthSizeClass.Compact) 1f else .5f),
            focusRequester = focusRequester,
            shape = LocalTheme.current.shapes.rectangularActionShape,
            hint = stringResource(Res.string.conversation_create_search_hint),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            prefixIcon = Icons.Outlined.Search,
            state = searchState,
            isClearable = true,
            showBorders = false
        )
    }
    items(
        items = membersToInvite.value.toList(),
        key = { "added_${it.id}" }
    ) { member ->
        val linkHandler = LocalLinkHandler.current

        NetworkItemRow(
            modifier = Modifier
                .animateItem()
                .wrapContentWidth()
                .padding(start = 8.dp)
                .scalingClickable(scaleInto = .95f) {
                    model.selectInvitedMember(member, add = false)
                    model.recommendUsersToInvite(query = searchState.text)
                },
            data = member.toNetworkItem(),
            onAvatarClick = {
                linkHandler?.invoke("/#/${member.userId}")
            }
        ) {
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.action_remove),
                tint = SharedColors.RED_ERROR_50
            )
        }
    }
    item(key = "addedHeader") {
        AnimatedVisibility(membersToInvite.value.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = LocalTheme.current.colors.secondary
                )
                Text(
                    text = stringResource(Res.string.conversation_selected_users),
                    style = LocalTheme.current.styles.category
                )
            }
        }
    }
    item(key = "title") {
        Text(
            modifier = Modifier.padding(bottom = 24.dp),
            text = stringResource(Res.string.conversation_create_title),
            style = LocalTheme.current.styles.subheading
        )
    }
}
