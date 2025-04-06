package ui.conversation

import NavigationHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import collectResult
import components.UserProfileImage
import data.io.base.AppPingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24

/** Screen displaying a conversation */
@Composable
fun ConversationScreen(
    conversationId: String? = null,
    name: String? = null
) {
    loadKoinModules(conversationModule)
    val viewModel: ConversationModel = koinViewModel(
        key = conversationId,
        parameters = {
            parametersOf(conversationId ?: "", true)
        }
    )
    val navController = LocalNavController.current
    val deviceType = LocalDeviceType.current

    val messages = viewModel.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = viewModel.conversation.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val reactingToMessageId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val showSettings = rememberSaveable {
        mutableStateOf(false)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        messages.refresh()
    }

    LaunchedEffect(Unit) {
        if(conversationId != null) viewModel.consumePing(conversationId)
    }

    LaunchedEffect(Unit) {
        viewModel.pingStream.collectLatest { stream ->
            stream.forEach {
                if(it.type == AppPingType.Conversation && it.identifiers.contains(conversationId)) {
                    messages.refresh()
                    viewModel.consumePing(it)
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

    Row {
        BrandBaseScreen(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(),
            navIconType = NavIconType.BACK,
            headerPrefix = {
                AnimatedVisibility(conversationDetail.value != null) {
                    Row {
                        UserProfileImage(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(32.dp),
                            media = conversationDetail.value?.summary?.avatar,
                            tag = conversationDetail.value?.summary?.tag,
                            animate = true,
                            name = conversationDetail.value?.summary?.alias
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
            title = name
        ) {
            Row(modifier = Modifier.align(Alignment.BottomCenter)) {
                ConversationComponent(
                    modifier = Modifier.weight(1f),
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
                    model = viewModel,
                    emptyLayout = {
                        // TODO
                    }
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSettings.value,
                    enter = slideInHorizontally { it * 2 },
                    exit = slideOutHorizontally { it * 2 }
                ) {
                    Box(modifier = Modifier.width(LocalScreenSize.current.width.times(.4f).dp)) {
                        MinimalisticIcon(
                            modifier = Modifier
                                .zIndex(1f)
                                .padding(top = 4.dp, end = 6.dp)
                                .align(Alignment.TopEnd),
                            imageVector = Icons.Outlined.Close,
                            tint = LocalTheme.current.colors.secondary,
                            onTap = {
                                showSettings.value = false
                            }
                        )
                        NavigationHost(startDestination = NavigationNode.ConversationSettings(conversationId))
                    }
                }
            }
        }
    }
}
