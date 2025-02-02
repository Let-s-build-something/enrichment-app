package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import augmy.composeapp.generated.resources.conversation_detail_you
import augmy.interactive.shared.ext.detectMessageInteraction
import augmy.interactive.shared.ext.horizontallyDraggable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.OnBackHandler
import augmy.interactive.shared.ui.components.navigation.ActionBarIcon
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.navigation.NavigationNode
import base.utils.LinkUtils
import base.utils.getOrNull
import base.utils.openLink
import components.UserProfileImage
import data.io.social.network.conversation.EmojiData
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MediaIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
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
import ui.conversation.components.TypingIndicator
import ui.conversation.components.audio.AudioMessageBubble
import ui.conversation.components.emoji.EmojiPreferencePicker
import ui.conversation.components.gif.GifImage
import ui.conversation.components.link.LinkPreview
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
        key = conversationId,
        parameters = { parametersOf(conversationId ?: "") }
    )

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = viewModel.conversationMessages.collectAsLazyPagingItems()
    val conversationDetail = viewModel.conversationDetail.collectAsState(initial = null)
    val preferredEmojis = viewModel.preferredEmojis.collectAsState()
    val isLoadingInitialPage = messages.loadState.refresh is LoadState.Loading
            || (messages.itemCount == 0 && !messages.loadState.append.endOfPaginationReached)
    val isEmpty = messages.itemCount == 0 && messages.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage

    val lastCurrentUserMessage = rememberSaveable(viewModel) {
        mutableStateOf(500)
    }
    val messagePanelHeight = rememberSaveable {
        mutableStateOf(100f)
    }
    val typingIndicatorsHeight = rememberSaveable {
        mutableStateOf(0f)
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
    val transcribedItem = remember {
        mutableStateOf<Pair<Int, String>?>(null) // index to id
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
            val typingIndicators = viewModel.typingIndicators.collectAsState()

            if(typingIndicators.value.second.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .animateContentSize()
                        .zIndex(5f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = messagePanelHeight.value.dp)
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .zIndex(5f)
                            .onSizeChanged {
                                if(it.height != 0) {
                                    with(density) { typingIndicatorsHeight.value = it.height.toDp().value }
                                }
                            }
                            .animateContentSize(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        typingIndicators.value.second.forEachIndexed { index, indicator ->
                            TypingIndicator(
                                modifier = Modifier
                                    .padding(start = 12.dp,)
                                    .fillMaxWidth(.8f)
                                    .clickable(indication = null, interactionSource = null) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                    },
                                key = typingIndicators.value.first,
                                data = indicator,
                                hasPrevious = index > 0,
                                hasNext = index < typingIndicators.value.second.lastIndex
                            )
                        }
                    }
                }
            }

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
                    .fillMaxWidth()
                    .wrapContentHeight()
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
                            .height(messagePanelHeight.value.dp + typingIndicatorsHeight.value.dp)
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
                    val data = messages.getOrNull(index)

                    val isCurrentUser = data?.authorPublicId == viewModel.currentUser.value?.publicId
                    val isPreviousMessageSameAuthor = messages.getOrNull(index + 1)?.authorPublicId == data?.authorPublicId
                    val nextItem = messages.getOrNull(index - 1)
                    val isNextMessageSameAuthor = nextItem?.authorPublicId == data?.authorPublicId

                    if(isCurrentUser && !isNextMessageSameAuthor && lastCurrentUserMessage.value > index) {
                        lastCurrentUserMessage.value = index
                    }
                    val isTranscribed = rememberSaveable(data?.id) { mutableStateOf(data?.transcribed == true) }

                    if(data?.id != null) {
                        LaunchedEffect(Unit) {
                            if(data.transcribed != true && (transcribedItem.value?.first ?: -1) < index) {
                                transcribedItem.value = index to data.id
                            }else if ((transcribedItem.value?.first ?: -1) == index
                                && data.id != transcribedItem.value?.second
                            ) {
                                transcribedItem.value = null
                            }

                            if(data.transcribed == true && transcribedItem.value?.second == data.id) {
                                transcribedItem.value = index + 1 to nextItem?.id.orEmpty()
                            }
                        }
                    }

                    DisposableEffect(null) {
                        onDispose {
                            if(transcribedItem.value?.second == data?.id) transcribedItem.value = null
                        }
                    }

                    MessageContent(
                        data = data?.copy(transcribed = data.transcribed == true || isTranscribed.value),
                        isPreviousMessageSameAuthor = isPreviousMessageSameAuthor,
                        isNextMessageSameAuthor = isNextMessageSameAuthor,
                        currentUser = isCurrentUser,
                        viewModel = viewModel,
                        reactingToMessageId = reactingToMessageId,
                        showEmojiPreferencesId = showEmojiPreferencesId,
                        replyToMessage = replyToMessage,
                        scrollToMessage = scrollToMessage,
                        preferredEmojis = preferredEmojis.value,
                        isMyLastMessage = lastCurrentUserMessage.value == index,
                        transcribe = /*!isCurrentUser &&*/ transcribedItem.value?.second == data?.id
                                && !isTranscribed.value,
                        onTranscribed = {
                            isTranscribed.value = true
                            viewModel.markMessageAsTranscribed(id = data?.id)
                            transcribedItem.value = index + 1 to nextItem?.id.orEmpty()
                        },
                        onReplyRequest = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index = 0)
                            }
                            replyToMessage.value = data
                        }
                    )
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

@Composable
private fun LazyItemScope.MessageContent(
    viewModel: ConversationViewModel,
    data: ConversationMessageIO?,
    isPreviousMessageSameAuthor: Boolean,
    isNextMessageSameAuthor: Boolean,
    currentUser: Boolean,
    isMyLastMessage: Boolean,
    transcribe: Boolean,
    onTranscribed: () -> Unit,
    reactingToMessageId: MutableState<String?>,
    showEmojiPreferencesId: MutableState<String?>,
    replyToMessage: MutableState<ConversationMessageIO?>,
    preferredEmojis: List<EmojiData>,
    scrollToMessage: (String?, Int?) -> Unit,
    onReplyRequest: () -> Unit
) {
    val density = LocalDensity.current
    val screenSize = LocalScreenSize.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    val isCurrentUser = if(data != null) currentUser else (0..1).random() == 0
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
        verticalAlignment = Alignment.Bottom
    ) {
        val profileImageSize = with(density) { 38.sp.toDp() }

        if(!isCurrentUser) {
            if(!isNextMessageSameAuthor) {
                UserProfileImage(
                    modifier = Modifier
                        .padding(
                            start = 12.dp,
                            end = 10.dp,
                            // offset if there are reactions (because those offset the message content)
                            bottom = if(!data?.reactions.isNullOrEmpty()) {
                                with(density) { LocalTheme.current.styles.category.fontSize.toDp() + 6.dp }
                            }else 0.dp
                        )
                        .zIndex(4f)
                        .size(profileImageSize),
                    model = data?.user?.photoUrl,
                    tag = data?.user?.tag
                )
            }else if(isPreviousMessageSameAuthor || isNextMessageSameAuthor) {
                Spacer(Modifier.width(profileImageSize + 22.dp))
            }
        }

        MessageBubble(
            data = data,
            isReacting = reactingToMessageId.value == data?.id,
            currentUserPublicId = viewModel.currentUser.value?.publicId ?: "",
            hasPrevious = isPreviousMessageSameAuthor,
            hasNext = isNextMessageSameAuthor,
            isReplying = replyToMessage.value?.id == data?.id,
            users = viewModel.conversationDetail.value?.users.orEmpty(),
            isMyLastMessage = isMyLastMessage,
            preferredEmojis = preferredEmojis,
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
                // TODO save in DB
            },
            transcribe = transcribe,
            onTranscribed = onTranscribed,
            onReplyRequest = onReplyRequest,
            additionalContent = { onDragChange, onDrag ->
                val rememberedHeight = rememberSaveable(data?.id) {
                    mutableStateOf(0f)
                }
                val shape = if(data?.content.isNullOrBlank()) {
                    LocalTheme.current.shapes.rectangularActionShape
                }else RoundedCornerShape(
                    topStart = LocalTheme.current.shapes.rectangularActionRadius,
                    topEnd = LocalTheme.current.shapes.rectangularActionRadius
                )
                val heightModifier = Modifier
                    .heightIn(
                        max = (screenSize.height.coerceAtMost(screenSize.width) * .7f).dp,
                        min = MEDIA_MAX_HEIGHT_DP.dp
                    )
                    .clip(shape)

                Column(
                    modifier = (if(rememberedHeight.value > 0f) Modifier.height(rememberedHeight.value.dp) else Modifier)
                        .wrapContentHeight()
                        .onSizeChanged {
                            if(it.height != 0) {
                                with(density) {
                                    rememberedHeight.value = it.height.toDp().value
                                }
                            }
                        }
                        .align(if(isCurrentUser) Alignment.End else Alignment.Start),
                    horizontalAlignment = if(isCurrentUser) Alignment.End else Alignment.Start
                ) {
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
                        val date = data.sentAt?.formatAsRelative() ?: ""

                        GifImage(
                            modifier = heightModifier
                                .zIndex(1f)
                                .pointerInput(data.id) {
                                    detectMessageInteraction(
                                        onTap = {
                                            coroutineScope.launch {
                                                navController?.navigate(
                                                    NavigationNode.MediaDetail(
                                                        media = listOf(
                                                            MediaIO(
                                                                url = data.gifAsset.original ?: "",
                                                                mimetype = "image/gif"
                                                            )
                                                        ),
                                                        title = if(isCurrentUser) {
                                                            getString(Res.string.conversation_detail_you)
                                                        } else data.user?.name,
                                                        subtitle = date
                                                    )
                                                )
                                            }
                                        },
                                        onLongPress = {
                                            reactingToMessageId.value = data.id
                                        },
                                        onDragChange = onDragChange,
                                        onDrag = onDrag
                                    )
                                },
                            data = data.gifAsset.original ?: "",
                            contentDescription = data.gifAsset.description,
                            contentScale = ContentScale.Fit
                        )
                    }
                    if(data?.media?.mapNotNull { m -> m.url.takeIf { !it.isNullOrBlank() } }?.isNotEmpty() == true) {
                        val date = data.sentAt?.formatAsRelative() ?: ""

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
                                .horizontallyDraggable(state = mediaRowState),
                            horizontalArrangement = Arrangement.spacedBy(
                                LocalTheme.current.shapes.betweenItemsSpace
                            )
                        ) {
                            (if(isCurrentUser) {
                                data.media
                            } else data.media.reversed()).forEachIndexed { index, media ->
                                val cachedMedia = viewModel.cachedFiles[media.url]
                                val canBeVisualized = media.mimetype?.contains("image") == true
                                        || media.mimetype?.contains("video") == true

                                MediaElement(
                                    modifier = if((data.state?.ordinal ?: 0) > 0 && canBeVisualized) {
                                        heightModifier.pointerInput(data.id, ) {
                                            detectMessageInteraction(
                                                onTap = {
                                                    coroutineScope.launch {
                                                        navController?.navigate(
                                                            NavigationNode.MediaDetail(
                                                                media = data.media,
                                                                selectedIndex = index,
                                                                title = if(isCurrentUser) {
                                                                    getString(Res.string.conversation_detail_you)
                                                                } else data.user?.name,
                                                                subtitle = date
                                                            )
                                                        )
                                                    }
                                                },
                                                onLongPress = {
                                                    reactingToMessageId.value = data.id
                                                },
                                                onDragChange = onDragChange,
                                                onDrag = onDrag
                                            )
                                        }
                                    }else heightModifier,
                                    media = media,
                                    localMedia = cachedMedia,
                                    enabled = false
                                )
                            }
                        }
                    }
                    if(!data?.audioUrl.isNullOrBlank()) {
                        AudioMessageBubble(
                            modifier = Modifier.zIndex(1f),
                            url = data?.audioUrl ?: "",
                            isCurrentUser = isCurrentUser,
                            hasPrevious = isPreviousMessageSameAuthor,
                            hasNext = isNextMessageSameAuthor
                        )
                    }

                    if(data?.showPreview == true && data.content?.isNotBlank() == true) {
                        val matches = remember {
                            LinkUtils.urlRegex.findAll(data.content)
                        }
                        if(matches.any()) {
                            matches.firstOrNull()?.let { firstLink ->
                                LinkPreview(
                                    modifier = Modifier
                                        .widthIn(max = (screenSize.width * .8f).dp)
                                        .pointerInput(data.id) {
                                            detectMessageInteraction(
                                                onTap = {
                                                    openLink(firstLink.value)
                                                },
                                                onDragChange = onDragChange,
                                                onDrag = onDrag
                                            )
                                        }
                                        .clip(shape = shape),
                                    url = firstLink.value,
                                    alignment = if(isCurrentUser) Alignment.Start else Alignment.End
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

/** Number of network items within one screen to be shimmered */
private const val MESSAGES_SHIMMER_ITEM_COUNT = 24
