package ui.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.LazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.conversation_detail_you
import augmy.interactive.shared.ext.verticallyDraggable
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.utils.PersistentListData
import augmy.interactive.shared.utils.persistedLazyListState
import base.navigation.NavigationNode
import base.utils.getOrNull
import data.io.social.network.conversation.message.ConversationMessageIO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.conversation.components.ConversationKeyboardMode
import ui.conversation.components.SendMessagePanel
import ui.conversation.components.TypingIndicator
import ui.conversation.components.emoji.EmojiPreferencePicker
import ui.conversation.components.message.MessageBubbleModel
import ui.conversation.message.ConversationMessageContent
import ui.conversation.message.UserVerificationMessage

/**
 * Component containing a list of messages derived from [messages] with shimmer loading effect which can be modified by [shimmerItemCount]
 * It also contains typing indicators, and messaging panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationComponent(
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
    model: ConversationModel,
    shimmerItemCount: Int = 20,
    verticalArrangement: Arrangement.Vertical,
    messages: LazyPagingItems<ConversationMessageIO>,
    conversationId: String?,
    thread: ConversationMessageIO? = null,
    lazyScope: LazyListScope.() -> Unit,
    emptyLayout: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = persistedLazyListState(
        persistentData = model.persistentPositionData ?: PersistentListData(),
        onDispose = { lastInfo ->
            model.persistentPositionData = lastInfo
        }
    )

    val preferredEmojis = model.preferredEmojis.collectAsState()
    val keyboardMode = rememberSaveable {
        mutableStateOf(ConversationKeyboardMode.Default.ordinal)
    }
    val transcribedItem = remember {
        mutableStateOf<Pair<Int, String>?>(null) // index to id
    }
    val reactingToMessageId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val messagePanelHeight = rememberSaveable {
        mutableStateOf(100f)
    }
    val typingIndicatorsHeight = rememberSaveable {
        mutableStateOf(0f)
    }
    val lastCurrentUserMessage = rememberSaveable(model) {
        mutableStateOf(Int.MAX_VALUE)
    }
    val showEmojiPreferencesId = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val replyToMessage = remember {
        mutableStateOf<ConversationMessageIO?>(null)
    }
    val isLoadingInitialPage = messages.loadState.refresh is LoadState.Loading
            || (messages.itemCount == 0 && !messages.loadState.append.endOfPaginationReached)
    val isEmpty = messages.itemCount == 0 && messages.loadState.append.endOfPaginationReached
            && !isLoadingInitialPage
    

    val scrollToMessage: (String?, Int?) -> Unit = { id, fallBackIndex ->
        val currentSnapshotList = messages.itemSnapshotList.toList()
        val index = currentSnapshotList.indexOfFirst { it?.id == id } + 2

        (index.takeIf { it != -1 } ?: fallBackIndex.takeIf { it != -1 })?.let { messageIndex ->
            coroutineScope.launch {
                listState.animateScrollToItem(messageIndex)
            }
        }
    }

    showEmojiPreferencesId.value?.let { messageId ->
        EmojiPreferencePicker(
            model = model,
            onEmojiSelected = { emoji ->
                model.reactToMessage(content = emoji, messageId = messageId)
                reactingToMessageId.value = null
                showEmojiPreferencesId.value = null
            },
            onDismissRequest = {
                showEmojiPreferencesId.value = null
            }
        )
    }

    Box(
        modifier = modifier
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
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val typingIndicators = model.typingIndicators.collectAsState(initial = 0 to listOf())

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
                    typingIndicators.value.second.forEach { indicator ->
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
                            data = indicator
                        )
                    }
                }
            }
        }

        content()

        LazyColumn(
            modifier = listModifier
                .verticallyDraggable(listState),
            reverseLayout = true,
            verticalArrangement = verticalArrangement,
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
                    emptyLayout()
                }
            }
            items(
                count = if(messages.itemCount == 0 && isLoadingInitialPage) shimmerItemCount else messages.itemCount,
                key = { index -> messages.getOrNull(index)?.id ?: index }
            ) { index ->
                val data = messages.getOrNull(index)

                val isCurrentUser = data?.authorPublicId == model.matrixUserId
                val isPreviousMessageSameAuthor = messages.getOrNull(index + 1)?.authorPublicId == data?.authorPublicId
                val nextItem = messages.getOrNull(index - 1)
                val isNextMessageSameAuthor = nextItem?.authorPublicId == data?.authorPublicId

                if(isCurrentUser && !isNextMessageSameAuthor && lastCurrentUserMessage.value > index) {
                    lastCurrentUserMessage.value = index
                }
                val isTranscribed = rememberSaveable(data?.id) { mutableStateOf(data?.transcribed == true) }

                if(data?.id != null && !isCurrentUser && !data.content.isNullOrBlank()) {
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

                val bubbleModel = remember(data?.id) {
                    object: MessageBubbleModel {
                        override val transcribe = derivedStateOf {
                            !isCurrentUser && transcribedItem.value?.second == data?.id
                                    && !isTranscribed.value
                        }

                        override fun onTranscribed() {
                            isTranscribed.value = true
                            model.markMessageAsTranscribed(id = data?.id)
                            transcribedItem.value = if(index > 0) {
                                index + 1 to nextItem?.id.orEmpty()
                            }else null
                        }

                        override fun onReactionRequest(isReacting: Boolean) {
                            reactingToMessageId.value = if(isReacting) data?.id else null
                        }

                        override fun onReactionChange(emoji: String) {
                            model.reactToMessage(content = emoji, messageId = data?.id)
                            reactingToMessageId.value = null
                        }

                        override fun onAdditionalReactionRequest() {
                            showEmojiPreferencesId.value = data?.id
                        }

                        override fun onReplyRequest() {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index = 0)
                            }
                            replyToMessage.value = data
                        }

                        override fun openDetail() {
                            if(thread == null) {
                                coroutineScope.launch {
                                    navController?.navigate(
                                        NavigationNode.MessageDetail(
                                            messageId = data?.id ?: "",
                                            conversationId = conversationId,
                                            title = if(isCurrentUser) {
                                                getString(Res.string.conversation_detail_you)
                                            } else data?.user?.content?.displayName
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if(data?.verification != null) {
                    UserVerificationMessage(data = data)
                } else {
                    ConversationMessageContent(
                        data = data?.copy(
                            transcribed = data.transcribed == true || isTranscribed.value,
                            anchorMessage = data.anchorMessage?.takeIf { it.id != thread?.id }
                        ),
                        isPreviousMessageSameAuthor = isPreviousMessageSameAuthor,
                        isNextMessageSameAuthor = isNextMessageSameAuthor,
                        currentUserPublicId = model.matrixUserId ?: "",
                        reactingToMessageId = reactingToMessageId,
                        model = bubbleModel,
                        replyToMessage = replyToMessage,
                        scrollToMessage = scrollToMessage,
                        preferredEmojis = preferredEmojis.value,
                        temporaryFiles = model.temporaryFiles.value.toMap(),
                        isMyLastMessage = lastCurrentUserMessage.value == index
                    )
                }
            }
            lazyScope()
        }

        SendMessagePanel(
            modifier = Modifier
                .fillMaxWidth()
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
            overrideAnchorMessage = thread,
            keyboardMode = keyboardMode,
            model = model,
            replyToMessage = replyToMessage,
            scrollToMessage = {
                scrollToMessage(it.id, -1)
            }
        )
    }
}