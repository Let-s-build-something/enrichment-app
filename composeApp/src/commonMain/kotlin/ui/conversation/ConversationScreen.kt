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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.rememberNavController
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.action_settings
import augmy.composeapp.generated.resources.conversation_mode_default
import augmy.composeapp.generated.resources.conversation_mode_experimental
import augmy.interactive.com.BuildKonfig
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.MultiChoiceSwitch
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.components.rememberMultiChoiceState
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import base.navigation.NestedNavigationBar
import collectResult
import components.UserProfileImage
import components.pull_refresh.LocalRefreshCallback
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.io.base.AppPingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.prototype.PrototypeConversation

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24

/** Screen displaying a conversation */
@Composable
fun ConversationScreen(
    conversationId: String? = null,
    name: String? = null
) {
    loadKoinModules(conversationModule)
    val model: ConversationModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId ?: "", true)
        }
    )
    val navController = LocalNavController.current
    val deviceType = LocalDeviceType.current

    val messages = model.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = model.conversation.collectAsState(initial = null)
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
                            media = conversationDetail.value?.summary?.avatar,
                            tag = conversationDetail.value?.tag,
                            animate = true,
                            name = conversationDetail.value?.summary?.roomName
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
            title = conversationDetail.value?.summary?.roomName ?: name
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
                        ConversationComponent(
                            listModifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            lazyScope = {
                                item(key = "topPadding") {
                                    Spacer(Modifier.height(42.dp))
                                }
                            },
                            messages = messages,
                            conversationId = conversationId,
                            shimmerItemCount = MESSAGES_SHIMMER_ITEM_COUNT,
                            model = model,
                            emptyLayout = {
                                // TODO
                            }
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
