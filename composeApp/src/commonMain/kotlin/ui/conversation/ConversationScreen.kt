package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.DateUtils.formatAsRelative
import augmy.interactive.shared.ext.horizontallyDraggable
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.getOrNull
import components.UserProfileImage
import data.io.social.network.conversation.ConversationMessageIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import ui.conversation.components.ConversationKeyboardMode
import ui.conversation.components.MEDIA_MAX_HEIGHT_DP
import ui.conversation.components.MediaElement
import ui.conversation.components.MessageBubble
import ui.conversation.components.ReplyIndication
import ui.conversation.components.SendMessagePanel
import ui.conversation.components.audio.AudioMessageBubble
import ui.conversation.components.emoji.EmojiPreferencePicker
import ui.conversation.components.gif.GifImage
import ui.conversation.components.rememberMessageBubbleState
import kotlin.math.abs
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

    val screenSize = LocalScreenSize.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = viewModel.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = viewModel.conversationDetail.collectAsState(initial = null)
    val preferredEmojis = viewModel.preferredEmojis.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState()
    val isLoadingInitialPage = messages.loadState.refresh is LoadState.Loading
            || (messages.itemCount == 0 && !messages.loadState.append.endOfPaginationReached)
    val isEmpty = messages.itemCount == 0 && messages.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage

    val messagePanelHeight = rememberSaveable {
        mutableStateOf(100f)
    }
    val keyboardMode = rememberSaveable {
        mutableStateOf(ConversationKeyboardMode.Default.ordinal)
    }
    val reactingToMessageId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val showEmojiPreferencesId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val replyToMessage = remember {
        mutableStateOf<ConversationMessageIO?>(null)
    }

    val scrollToMessage: (String?, Int?) -> Unit = { id, fallBackIndex ->
        val currentSnapshotList = messages.itemSnapshotList.toList()
        val index = currentSnapshotList.indexOfFirst { it?.id == id }

        (index.takeIf { it != -1 } ?: fallBackIndex.takeIf { it != -1 })?.let { messageIndex ->
            coroutineScope.launch {
                listState.animateScrollToItem(messageIndex)
            }
        }
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
                            when {
                                showEmojiPreferencesId.value != null -> showEmojiPreferencesId.value = null
                                keyboardMode.value != ConversationKeyboardMode.Default.ordinal -> {
                                    keyboardMode.value = ConversationKeyboardMode.Default.ordinal
                                }
                                else -> {
                                    focusManager.clearFocus()
                                    reactingToMessageId.value = null
                                }
                            }
                        })
                    }
                    .align(Alignment.BottomCenter)
                    .fillMaxSize()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                listState.scrollBy(delta)
                            }
                        },
                    ),
                verticalArrangement = Arrangement.Bottom,
                reverseLayout = true,
                state = listState
            ) {
                item(key = "navigationPadding") {
                    Spacer(
                        Modifier
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .height(messagePanelHeight.value.dp)
                            .animateContentSize()
                    )
                }
                item(key = "emptyLayout") {
                    AnimatedVisibility(
                        enter = expandVertically() + fadeIn(),
                        visible = isEmpty
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
                        val scrollPosition = rememberSaveable(data?.id) {
                            mutableStateOf(0)
                        }
                        val mediaRowState = rememberScrollState(
                            initial = scrollPosition.value
                        )
                        if(scrollPosition.value != 0) {
                            LaunchedEffect(Unit) {
                                delay(400)
                                mediaRowState.animateScrollBy(scrollPosition.value.toFloat())
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if(isPreviousMessageSameAuthor) 1.dp else LocalTheme.current.shapes.betweenItemsSpace.div(2),
                                    bottom = if(isNextMessageSameAuthor) 1.dp else LocalTheme.current.shapes.betweenItemsSpace.div(2)
                                )
                                .animateItem(),
                            horizontalArrangement = if(isCurrentUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val profileImageSize = with(density) { 38.sp.toDp() }
                            val isLastOfStack = !isCurrentUser && !isNextMessageSameAuthor
                            if(isLastOfStack) {
                                UserProfileImage(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .zIndex(4f)
                                        .size(profileImageSize),
                                    model = data?.user?.photoUrl,
                                    tag = data?.user?.tag
                                )
                            }

                            MessageBubble(
                                modifier = Modifier
                                    .padding(
                                        start = LocalTheme.current.shapes.betweenItemsSpace.plus(
                                            if (!isLastOfStack && (isPreviousMessageSameAuthor || isNextMessageSameAuthor)) {
                                                12.dp + profileImageSize
                                            } else 0.dp
                                        ).plus(
                                            if (isCurrentUser) 50.dp else 0.dp
                                        )
                                    )
                                    .padding(
                                        start = if (isCurrentUser) 16.dp else 0.dp,
                                        end = if (isCurrentUser) 0.dp else 16.dp,
                                    ),
                                data = data,
                                isReacting = reactingToMessageId.value == data?.id,
                                currentUserPublicId = currentUser.value?.publicId ?: "",
                                hasPrevious = isPreviousMessageSameAuthor,
                                hasNext = isNextMessageSameAuthor,
                                isReplying = replyToMessage.value?.id == data?.id,
                                users = conversationDetail.value?.users.orEmpty(),
                                preferredEmojis = preferredEmojis.value,
                                state = rememberMessageBubbleState(
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
                                    },
                                    onReplyRequest = {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(index = 0)
                                        }
                                        replyToMessage.value = data
                                    }
                                ),
                                additionalContent = {
                                    val heightModifier = Modifier.heightIn(
                                        max = (screenSize.height.coerceAtMost(screenSize.width) * .7f).dp,
                                        min = MEDIA_MAX_HEIGHT_DP.dp
                                    )

                                    data?.anchorMessage?.let { anchorData ->
                                        ReplyIndication(
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .padding(start = 12.dp),
                                            data = anchorData,
                                            onClick = {
                                                scrollToMessage(anchorData.id, anchorData.index)
                                            },
                                            isCurrentUser = anchorData.authorPublicId == viewModel.currentUser.value?.publicId
                                        )
                                    }
                                    if(data?.gifAsset != null) {
                                        val date = data.createdAt?.formatAsRelative() ?: ""

                                        GifImage(
                                            modifier = heightModifier
                                                .align(Alignment.End)
                                                .zIndex(1f)
                                                .scalingClickable(
                                                    scaleInto = .95f,
                                                    onLongPress = {
                                                        reactingToMessageId.value = data.id
                                                    }
                                                ) {
                                                    navController?.navigate(
                                                        NavigationNode.MediaDetail(
                                                            urls = listOf(data.gifAsset.original ?: ""),
                                                            title = data.user?.displayName,
                                                            subtitle = date
                                                        )
                                                    )
                                                }
                                                .clip(RoundedCornerShape(6.dp)),
                                            data = data.gifAsset.original ?: "",
                                            contentDescription = data.gifAsset.description,
                                            contentScale = ContentScale.FillHeight
                                        )
                                    }
                                    if(data?.mediaUrls?.mapNotNull { m -> m.takeIf { it.isNotBlank() } }?.isNotEmpty() == true) {
                                        val date = data.createdAt?.formatAsRelative() ?: ""

                                        LaunchedEffect(mediaRowState) {
                                            snapshotFlow { mediaRowState.value }.collect {
                                                if(abs(scrollPosition.value - it) < 300) {
                                                    scrollPosition.value = it
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = heightModifier
                                                .wrapContentWidth()
                                                .horizontalScroll(state = mediaRowState)
                                                .horizontallyDraggable(state = mediaRowState)
                                                .animateContentSize()
                                        ) {
                                            if(isCurrentUser) {
                                                Spacer(Modifier.width((screenSize.width * .3f).dp))
                                            }
                                            (if(isCurrentUser) {
                                                data.mediaUrls
                                            } else data.mediaUrls.reversed()).forEachIndexed { index, mediaUrl ->
                                                val media = viewModel.cachedFiles[mediaUrl]

                                                MediaElement(
                                                    modifier = heightModifier
                                                        .padding(
                                                            horizontal = LocalTheme.current.shapes.betweenItemsSpace / 2
                                                        )
                                                        .scalingClickable(
                                                            enabled = (data.state?.ordinal ?: 0) > 0,
                                                            scaleInto = .95f,
                                                            onLongPress = {
                                                                reactingToMessageId.value = data.id
                                                            }
                                                        ) {
                                                            navController?.navigate(
                                                                NavigationNode.MediaDetail(
                                                                    urls = data.mediaUrls,
                                                                    selectedIndex = index,
                                                                    title = data.user?.displayName,
                                                                    subtitle = date
                                                                )
                                                            )
                                                        }
                                                        .clip(LocalTheme.current.shapes.rectangularActionShape),
                                                    url = mediaUrl,
                                                    media = media
                                                )
                                            }
                                            if(!isCurrentUser) {
                                                Spacer(Modifier.width((screenSize.width * .3f).dp))
                                            }
                                        }
                                    }
                                    if(!data?.audioUrl.isNullOrBlank()) {
                                        AudioMessageBubble(
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .align(Alignment.End)
                                                .zIndex(1f),
                                            url = data?.audioUrl ?: "",
                                            isCurrentUser = isCurrentUser,
                                            hasPrevious = isPreviousMessageSameAuthor,
                                            hasNext = isNextMessageSameAuthor
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                item(key = "topPadding") {
                    Spacer(Modifier.height(42.dp))
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
                keyboardMode = keyboardMode,
                viewModel = viewModel,
                replyToMessage = replyToMessage,
                scrollToMessage = {
                    scrollToMessage(it.id, -1)
                }
            )
        }
    }
}

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24
