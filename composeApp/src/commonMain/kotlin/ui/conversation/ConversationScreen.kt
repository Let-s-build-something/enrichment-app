package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationArguments
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
import ui.conversation.components.emoji.EmojiPreferencePicker

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24

/** Screen displaying a conversation */
@OptIn(ExperimentalMaterial3Api::class)
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

    val messages = viewModel.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = viewModel.conversationDetail.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val reactingToMessageId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val showEmojiPreferencesId = rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        //messages.refresh()
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

    showEmojiPreferencesId.value?.let { messageId ->
        EmojiPreferencePicker(
            viewModel = viewModel,
            onEmojiSelected = { emoji ->
                viewModel.reactToMessage(content = emoji, messageId = messageId)
                reactingToMessageId.value = null
                showEmojiPreferencesId.value = null
            },
            onDismissRequest = {
                showEmojiPreferencesId.value = null
            }
        )
    }

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
                        tag = conversationDetail.value?.summary?.tag,
                        animate = true
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
                    // TODO hamburger menu
                }
            )
        },
        clearFocus = false,
        title = name
    ) {
        ConversationComponent(
            listModifier = Modifier
                .align(Alignment.BottomCenter)
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
            viewModel = viewModel,
            emptyLayout = {
                // TODO
            }
        )
    }
}
