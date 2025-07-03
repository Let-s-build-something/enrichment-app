package ui.conversation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import augmy.composeapp.generated.resources.conversation_creating_room
import augmy.composeapp.generated.resources.conversation_detail_you
import augmy.composeapp.generated.resources.conversation_no_room_message
import augmy.composeapp.generated.resources.conversation_no_room_title
import augmy.interactive.shared.ext.draggable
import augmy.interactive.shared.ext.ifNull
import augmy.interactive.shared.ui.base.LocalLinkHandler
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.utils.PersistentListData
import augmy.interactive.shared.utils.persistedLazyListState
import base.navigation.NavigationNode
import base.utils.getOrNull
import base.utils.openLink
import components.EmptyLayout
import data.io.social.network.conversation.message.FullConversationMessage
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.conversation.ConversationRepository.Companion.MENTION_REGEX_USER_ID
import ui.conversation.components.ConversationKeyboardMode
import ui.conversation.components.SendMessagePanel
import ui.conversation.components.SystemMessage
import ui.conversation.components.TypingIndicator
import ui.conversation.components.emoji.EmojiPreferencePicker
import ui.conversation.components.message.MessageBubbleModel
import ui.conversation.message.AUTHOR_SYSTEM
import ui.conversation.message.ConversationMessageContent
import ui.conversation.message.MessageType
import ui.conversation.message.user_verification.UserVerificationMessage
import ui.network.components.user_detail.UserDetailDialog

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
    listState: LazyListState = persistedLazyListState(
        persistentData = model.persistentPositionData ?: PersistentListData(),
        onDispose = { lastInfo ->
            model.persistentPositionData = lastInfo
        }
    ),
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    messages: LazyPagingItems<FullConversationMessage>,
    conversationId: String?,
    highlight: String? = null,
    thread: FullConversationMessage? = null,
    lazyScope: LazyListScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

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
        mutableStateOf<FullConversationMessage?>(null)
    }
    val clickedUserId = remember { mutableStateOf<String?>(null) }

    val isLoadingInitialPage = messages.loadState.refresh is LoadState.Loading
            || (messages.itemCount == 0 && !messages.loadState.append.endOfPaginationReached)

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
        val uiMode = model.uiMode.collectAsState()

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

        clickedUserId.value?.let {
            UserDetailDialog(
                userId = it,
                onDismissRequest = { clickedUserId.value = null }
            )
        }

        CompositionLocalProvider(
            LocalLinkHandler provides { href ->
                coroutineScope.launch(Dispatchers.Default) {
                    MENTION_REGEX_USER_ID.toRegex().find(href)?.groupValues[1]?.let { userId ->
                        clickedUserId.value = userId
                    }.ifNull {
                        openLink(href)
                    }
                }
            }
        ) {
            LazyColumn(
                modifier = listModifier.draggable(listState),
                reverseLayout = true,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
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
                item(key = "uiModeIndication") {
                    Crossfade(
                        modifier = Modifier.animateContentSize(),
                        targetState = uiMode.value
                    ) { mode ->
                        when(mode) {
                            ConversationModel.UiMode.IdleNoRoom -> EmptyLayout(
                                modifier = Modifier.padding(bottom = 32.dp),
                                title = stringResource(Res.string.conversation_no_room_title),
                                description = stringResource(Res.string.conversation_no_room_message)
                            )
                            ConversationModel.UiMode.CreatingRoom -> EmptyLayout(
                                modifier = Modifier.padding(bottom = 32.dp),
                                title = stringResource(Res.string.conversation_creating_room),
                                animSpec = {
                                    LottieCompositionSpec.DotLottie(Res.readBytes("files/loading_envelope.lottie"))
                                }
                            )
                            ConversationModel.UiMode.InternalError -> {}
                            else -> {}
                        }
                    }
                }
                items(
                    count = (if(messages.itemCount == 0 && isLoadingInitialPage) shimmerItemCount else messages.itemCount).takeIf {
                        uiMode.value == ConversationModel.UiMode.Idle
                    } ?: 0,
                    key = { index -> messages.getOrNull(index)?.id ?: index }
                ) { index ->
                    val data = messages.getOrNull(index)

                    val messageType = when {
                        data?.data?.authorPublicId == model.matrixUserId -> MessageType.CurrentUser
                        data?.data?.authorPublicId == AUTHOR_SYSTEM -> MessageType.System
                        data == null -> if((0..1).random() == 0) MessageType.CurrentUser else MessageType.OtherUser
                        else -> MessageType.OtherUser
                    }

                    if(messageType == MessageType.System) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if(data?.data?.verification != null) {
                                UserVerificationMessage(data = data)
                            }else {
                                SystemMessage(data = data)
                            }
                        }
                    }else {
                        val isPreviousMessageSameAuthor = messages.getOrNull(index + 1)?.data?.authorPublicId == data?.data?.authorPublicId
                        val nextItem = messages.getOrNull(index - 1)
                        val isNextMessageSameAuthor = nextItem?.data?.authorPublicId == data?.data?.authorPublicId

                        LaunchedEffect(Unit) {
                            if(messageType == MessageType.CurrentUser
                                && !isNextMessageSameAuthor
                                && lastCurrentUserMessage.value > index
                            ) {
                                lastCurrentUserMessage.value = index
                            }
                        }
                        val isTranscribed = rememberSaveable(data?.id) { mutableStateOf(data?.data?.transcribed == true) }

                        if(data?.id != null && messageType == MessageType.OtherUser && !data.data.content.isNullOrBlank()) {
                            LaunchedEffect(Unit) {
                                if(data.data.transcribed != true && (transcribedItem.value?.first ?: -1) < index) {
                                    transcribedItem.value = index to data.id
                                }else if ((transcribedItem.value?.first ?: -1) == index
                                    && data.id != transcribedItem.value?.second
                                ) {
                                    transcribedItem.value = null
                                }

                                if(data.data.transcribed == true && transcribedItem.value?.second == data.id) {
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
                                    messageType == MessageType.OtherUser
                                            && transcribedItem.value?.second == data?.id
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
                                                    title = if(messageType == MessageType.CurrentUser) {
                                                        getString(Res.string.conversation_detail_you)
                                                    } else data?.author?.displayName
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ConversationMessageContent(
                            data = data?.copy(
                                anchorMessage = data.anchorMessage?.takeIf { it.id != thread?.id },
                                data = data.data.copy(
                                    transcribed = data.data.transcribed == true || isTranscribed.value,
                                )
                            ),
                            temporaryFiles = model.temporaryFiles.value.toMap(),
                            currentUserPublicId = model.matrixUserId ?: "",
                            highlight = highlight,
                            isPreviousMessageSameAuthor = isPreviousMessageSameAuthor,
                            isNextMessageSameAuthor = isNextMessageSameAuthor,
                            messageType = messageType,
                            isMyLastMessage = lastCurrentUserMessage.value == index,
                            model = bubbleModel,
                            reactingToMessageId = reactingToMessageId,
                            replyToMessage = replyToMessage,
                            preferredEmojis = preferredEmojis.value,
                            scrollToMessage = { messageId ->
                                model.scrollTo(messageId)
                            }
                        )
                    }
                }
                lazyScope()
            }
        }

        SendMessagePanel(
            modifier = Modifier
                .fillMaxWidth()
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
                model.scrollTo(it.id)
            }
        )
    }
}