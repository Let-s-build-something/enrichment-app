package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.utils.getOrNull
import components.UserProfileImage
import components.conversation.MessageBubble
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Screen displaying a conversation */
@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String? = null,
    name: String? = null
) {
    loadKoinModules(conversationModule)
    val viewModel: ConversationViewModel = koinViewModel(
        parameters = { parametersOf(conversationId ?: "") }
    )

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    val messages = viewModel.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = viewModel.conversationDetail.collectAsState(initial = null)
    val preferredEmojis = viewModel.preferredEmojis.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState()
    val isLoadingInitialPage = messages.loadState.refresh is LoadState.Loading

    val messagePanelHeight = rememberSaveable {
        mutableStateOf(100f)
    }
    val isEmojiPickerVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val reactingToMessageId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val showEmojiPreferencesId = rememberSaveable {
        mutableStateOf<String?>(null)
    }

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
                        model = conversationDetail.value?.pictureUrl,
                        tag = conversationDetail.value?.tag,
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            reactingToMessageId.value = null
                            isEmojiPickerVisible.value = false
                        })
                    }
                    .align(Alignment.BottomCenter)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                reverseLayout = true
            ) {
                item {
                    Spacer(
                        Modifier
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .height(messagePanelHeight.value.dp)
                            .animateContentSize()
                    )
                }
                item {
                    AnimatedVisibility(
                        enter = expandVertically() + fadeIn(),
                        visible = messages.itemCount == 0 && !isLoadingInitialPage
                    ) {
                        // TODO empty layout
                    }
                }
                items(
                    count = if(messages.itemCount == 0 && isLoadingInitialPage) MESSAGES_SHIMMER_ITEM_COUNT else messages.itemCount,
                    key = { index -> messages.getOrNull(index)?.id ?: Uuid.random().toString() }
                ) { index ->
                    messages.getOrNull(index).let { data ->
                        val isCurrentUser = if(data != null) {
                            data.authorPublicId == currentUser.value?.publicId
                        }else (0..1).random() == 0
                        val isPreviousMessageSameAuthor = messages.getOrNull(index + 1)?.authorPublicId == data?.authorPublicId
                        val isNextMessageSameAuthor = messages.getOrNull(index - 1)?.authorPublicId == data?.authorPublicId

                        Row(
                            modifier = Modifier
                                .padding(start = if(isCurrentUser) 50.dp else 12.dp)
                                .fillMaxWidth()
                                .padding(
                                    top = if(isPreviousMessageSameAuthor) 1.dp else LocalTheme.current.shapes.betweenItemsSpace.div(2),
                                    bottom = if(isNextMessageSameAuthor) 1.dp else LocalTheme.current.shapes.betweenItemsSpace.div(2)
                                )
                                .animateItem(),
                            horizontalArrangement = if(isCurrentUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = if(isPreviousMessageSameAuthor) Alignment.Top else Alignment.Bottom
                        ) {
                            val profileImageSize = with(density) { 38.sp.toDp() }
                            if(!isCurrentUser && !isNextMessageSameAuthor) {
                                UserProfileImage(
                                    modifier = Modifier.size(profileImageSize),
                                    model = data?.user?.photoUrl,
                                    tag = data?.user?.tag
                                )
                                Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                            }else if(isPreviousMessageSameAuthor || isNextMessageSameAuthor) {
                                Spacer(Modifier.width(
                                    LocalTheme.current.shapes.betweenItemsSpace + profileImageSize
                                ))
                            }
                            MessageBubble(
                                data = data,
                                isReacting = reactingToMessageId.value == data?.id,
                                currentUserPublicId = currentUser.value?.publicId ?: "",
                                hasPrevious = isPreviousMessageSameAuthor,
                                hasNext = isNextMessageSameAuthor,
                                users = conversationDetail.value?.users.orEmpty(),
                                preferredEmojis = preferredEmojis.value,
                                onReactionRequest = { show ->
                                    reactingToMessageId.value = if(show) data?.id else null
                                },
                                onReactionChange = { emoji ->
                                    if(data?.id != null) {
                                        viewModel.reactToMessage(content = emoji, messageId = data.id)
                                        reactingToMessageId.value = null
                                    }
                                },
                                onAdditionalReactionRequest = {
                                    showEmojiPreferencesId.value = data?.id
                                }
                            )
                        }
                    }
                }
            }
            SendMessagePanel(
                modifier = Modifier
                    .background(
                        color = LocalTheme.current.colors.backgroundDark,
                        shape = RoundedCornerShape(
                            topStart = LocalTheme.current.shapes.componentCornerRadius,
                            topEnd = LocalTheme.current.shapes.componentCornerRadius
                        )
                    )
                    .onSizeChanged {
                        if(it.height != 0) {
                            with(density) {
                                messagePanelHeight.value = it.height.toDp().value
                            }
                        }
                    },
                isEmojiPickerVisible = isEmojiPickerVisible,
                viewModel = viewModel
            )
        }
    }
}

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24
