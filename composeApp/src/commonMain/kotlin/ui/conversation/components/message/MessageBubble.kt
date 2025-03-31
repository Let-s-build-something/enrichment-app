package ui.conversation.components.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_message_react
import augmy.composeapp.generated.resources.accessibility_message_download
import augmy.composeapp.generated.resources.accessibility_message_reply
import augmy.composeapp.generated.resources.accessibility_reaction_other
import augmy.composeapp.generated.resources.message_read_more
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.detectMessageInteraction
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalIsMouseUser
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.theme.Colors
import base.theme.DefaultThemeStyles.Companion.fontQuicksandMedium
import base.theme.DefaultThemeStyles.Companion.fontQuicksandSemiBold
import base.utils.openLink
import components.buildAnnotatedLinkString
import data.io.social.network.conversation.ConversationTypingIndicator
import data.io.social.network.conversation.EmojiData
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.social.network.conversation.message.MessageState
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.components.LoadingMessageBubble
import ui.conversation.components.audio.MediaProcessorModel
import ui.conversation.components.experimental.pacing.buildTempoString
import ui.conversation.media.DownloadIndication
import ui.conversation.media.rememberIndicationState
import kotlin.math.absoluteValue


interface MessageBubbleModel {
    val transcribe: State<Boolean>

    fun onTranscribed()
    fun onReactionRequest(isReacting: Boolean)
    fun onReactionChange(emoji: String)
    fun onAdditionalReactionRequest()
    fun onReplyRequest()
    fun openDetail()
}

/**
 * Horizontal bubble displaying textual content of a message and its reactions
 * @param isMyLastMessage whether this message is the last of the current user overall
 */
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO?,
    model: MessageBubbleModel,
    isReacting: Boolean,
    preferredEmojis: List<EmojiData>,
    hasPrevious: Boolean,
    hasNext: Boolean,
    isMyLastMessage: Boolean,
    isReplying: Boolean,
    currentUserPublicId: String,
    additionalContent: @Composable ColumnScope.(
        onDragChange: (PointerInputChange, Offset) -> Unit,
        onDrag: (Boolean) -> Unit,
        messageContent: AnnotatedString
    ) -> Unit
) {
    Crossfade(targetState = data == null) { isLoading ->
        if(isLoading) {
            ShimmerLayout(modifier = modifier)
        }else if(data != null) {
            ContentLayout(
                modifier = modifier,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                data = data,
                model = model,
                preferredEmojis = preferredEmojis,
                currentUserPublicId = currentUserPublicId,
                isReacting = isReacting,
                isReplying = isReplying,
                isMyLastMessage = isMyLastMessage,
                additionalContent = additionalContent
            )
        }
    }
}

@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO,
    preferredEmojis: List<EmojiData>,
    hasPrevious: Boolean,
    isMyLastMessage: Boolean,
    hasNext: Boolean,
    model: MessageBubbleModel,
    isReplying: Boolean,
    currentUserPublicId: String,
    isReacting: Boolean,
    additionalContent: @Composable ColumnScope.(
        onDragChange: (PointerInputChange, Offset) -> Unit,
        onDrag: (Boolean) -> Unit,
        messageContent: AnnotatedString
    ) -> Unit
) {
    val density = LocalDensity.current
    val screenSize = LocalScreenSize.current
    val isMouseUser = LocalIsMouseUser.current
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact
    val coroutineScope = rememberCoroutineScope()
    val dragCoroutineScope = rememberCoroutineScope()
    val isCurrentUser = data.authorPublicId == currentUserPublicId

    val replyBounds = remember {
        with(density) {
            (-screenSize.width.dp.toPx() / 8f)..(screenSize.width.dp.toPx() / 8f)
        }
    }
    val contentPadding = PaddingValues(
        bottom = if(!data.reactions.isNullOrEmpty()) {
            with(density) { LocalTheme.current.styles.category.fontSize.toDp() + 6.dp }
        }else 0.dp
    )
    val replyIndicationSize = with(density) { LocalTheme.current.styles.category.fontSize.toDp() + 20.dp }
    val hoverInteractionSource = remember(data.id) { MutableInteractionSource() }
    val processor = if(data.media?.isEmpty() == false) koinViewModel<MediaProcessorModel>(key = data.id) else null
    val downloadState = if(processor != null) rememberIndicationState(processor) else null
    val hasAttachment = remember(data.id) { data.media?.isEmpty() == false || data.containsUrl }
    val isFocused = hoverInteractionSource.collectIsHoveredAsState()
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val awaitingTranscription = !isCurrentUser
            && !model.transcribe.value
            && data.transcribed != true
            && !data.timings.isNullOrEmpty()

    val textContent = if(!data.content.isNullOrBlank()) {
        buildTempoString(
            key = data.id,
            timings = data.timings.orEmpty(),
            text = buildAnnotatedLinkString(
                text = data.content,
                onLinkClicked = { openLink(it) }
            ),
            onFinish = { model.onTranscribed() },
            enabled = model.transcribe.value,
            style = LocalTheme.current.styles.title.copy(
                color = (if (isCurrentUser) Colors.GrayLight else LocalTheme.current.colors.secondary)
                    .copy(
                        alpha = if(awaitingTranscription) .4f else 1f
                    ),
                fontFamily = FontFamily(fontQuicksandMedium)
            )
        )
    }else AnnotatedString("")

    val showDetailDialogOf = remember(data.id) {
        mutableStateOf<Pair<String?, String?>?>(null)
    }
    val isDragged = remember(data.id) {
        mutableStateOf(false)
    }
    val animatedOffsetX = remember(data.id) {
        Animatable(0f)
    }
    val offsetX = remember(data.id) {
        mutableStateOf(0f)
    }
    val verticalPadding = animateFloatAsState(
        targetValue = if(isReacting) 32f else 0f,
        label = "verticalPaddingAnimation"
    )
    val additionalOffsetDp = animateFloatAsState(
        targetValue = if (isReplying) {
            if(isCurrentUser) - replyIndicationSize.value - 4f else replyIndicationSize.value + 4f
        } else 0f,
        label = "startPaddingAnimation"
    )
    val onDownloadRequest: () -> Unit = {
        processor?.downloadFiles(
            *data.media.orEmpty().toTypedArray()
        )
    }
    val onDrag: (Boolean) -> Unit = { dragged ->
        isDragged.value = dragged

        // cancel dragging and animate back to original position
        dragCoroutineScope.coroutineContext.cancelChildren()
        if(!dragged) {
            if(animatedOffsetX.value !in replyBounds) {
                coroutineScope.launch {
                    model.onReplyRequest()
                    offsetX.value = 0f
                    animatedOffsetX.animateTo(0f)
                }
            }else {
                dragCoroutineScope.launch {
                    delay(DragCancelDelayMillis)
                    offsetX.value = 0f
                    animatedOffsetX.animateTo(0f)
                }
            }
        }
    }
    val onDragChange: (PointerInputChange, Offset) -> Unit = { _, dragAmount ->
        offsetX.value = (offsetX.value + dragAmount.x / 3).coerceIn(
            minimumValue = if(isCurrentUser) replyBounds.start.times(1.4f) else 0f,
            maximumValue = if(isCurrentUser) 0f else replyBounds.endInclusive.times(1.4f)
        )
        coroutineScope.launch {
            animatedOffsetX.animateTo(offsetX.value)
        }
    }

    showDetailDialogOf.value?.let {
        MessageReactionsDialog(
            reactions = data.reactions.orEmpty(),
            messageContent = it.first,
            initialEmojiSelection = it.second,
            onDismissRequest = {
                showDetailDialogOf.value = null
            }
        )
    }

    // everything + message footer information
    Row(
        modifier = modifier
            .hoverable(
                enabled = !isCompact,
                interactionSource = hoverInteractionSource
            )
            .padding(vertical = verticalPadding.value.dp)
            .offset(
                x = with(density) { animatedOffsetX.value.toDp() } + additionalOffsetDp.value.dp
            )
            .pointerInput(data.id, isReacting) {
                detectMessageInteraction(
                    onTap = {
                        if(isReacting) model.onReactionRequest(false)
                        else model.openDetail()
                    },
                    onLongPress = {
                        model.onReactionRequest(true)
                    },
                    onDrag = onDrag,
                    onDragChange = onDragChange
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = alignment
        ) {
            Box {
                // reply indication
                if (animatedOffsetX.value.absoluteValue > 0f || isReplying) {
                    val percentageAchieved = (if (isCurrentUser) {
                        animatedOffsetX.value / replyBounds.start
                    } else animatedOffsetX.value / replyBounds.endInclusive).times(2)

                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .offset(
                                x = (if (isCurrentUser) replyIndicationSize + 4.dp else -replyIndicationSize - 4.dp).times(
                                    if (isReplying) 1f else percentageAchieved.coerceAtMost(
                                        1f
                                    )
                                )
                            )
                            .align(if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.requiredSize(replyIndicationSize),
                            progress = { percentageAchieved / 2 },
                            strokeWidth = 4.dp,
                            color = LocalTheme.current.colors.component,
                            trackColor = Color.Transparent
                        )
                        Icon(
                            modifier = Modifier
                                .size(replyIndicationSize)
                                .then(
                                    if (animatedOffsetX.value !in replyBounds) {
                                        Modifier.background(
                                            color = LocalTheme.current.colors.component,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .padding(5.dp),
                            imageVector = Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = stringResource(Res.string.accessibility_message_reply),
                            tint = LocalTheme.current.colors.secondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.animateContentSize(
                        alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val showOptions = isReacting && !isReplying

                    if(isCurrentUser) {
                        Options(
                            modifier = Modifier
                                .padding(contentPadding)
                                .padding(end = 8.dp),
                            visible = !showOptions && isFocused.value,
                            hasMedia = data.media?.isEmpty() == false,
                            onDownloadRequest = onDownloadRequest,
                            onReplyRequest = { model.onReplyRequest() },
                            onReactionRequest = { model.onReactionRequest(it) }
                        )
                    }

                    // message content + reply function + reactions
                    Box(
                        (if (isReacting || data.anchorMessage != null) {
                            Modifier.background(
                                color = LocalTheme.current.colors.backgroundDark,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                        } else Modifier)
                            .animateContentSize(
                                alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh)
                            )
                    ) {
                        Column(
                            horizontalAlignment = alignment,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // new or a change of a reaction - indication
                            AnimatedVisibility(isReacting) {
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            vertical = 10.dp,
                                            horizontal = 12.dp
                                        )
                                        .horizontalScroll(rememberScrollState())
                                        .zIndex(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    preferredEmojis.forEach { emojiData ->
                                        Text(
                                            modifier = Modifier
                                                .scalingClickable(scaleInto = .7f) {
                                                    model.onReactionChange(emojiData.emoji.firstOrNull() ?: "")
                                                }
                                                .padding(8.dp),
                                            text = emojiData.emoji.firstOrNull() ?: "",
                                            style = LocalTheme.current.styles.heading
                                        )
                                    }
                                    Icon(
                                        modifier = Modifier
                                            .size(with(density) { LocalTheme.current.styles.heading.fontSize.toDp() } + 6.dp)
                                            .scalingClickable {
                                                model.onAdditionalReactionRequest()
                                            },
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = stringResource(Res.string.accessibility_reaction_other),
                                        tint = LocalTheme.current.colors.secondary
                                    )
                                }
                            }

                            val messageShape = if (isCurrentUser) {
                                RoundedCornerShape(
                                    topStart = if(hasAttachment) 1.dp else 24.dp,
                                    topEnd = if(hasPrevious || !data.media.isNullOrEmpty() || hasAttachment) 1.dp else 24.dp,
                                    bottomStart = 24.dp,
                                    bottomEnd = if (hasNext) 1.dp else 24.dp
                                )
                            } else {
                                RoundedCornerShape(
                                    topEnd = if(hasAttachment) 1.dp else 24.dp,
                                    topStart = if(hasPrevious || !data.media.isNullOrEmpty() || hasAttachment) 1.dp else 24.dp,
                                    bottomEnd = 24.dp,
                                    bottomStart = if (hasNext) 1.dp else 24.dp
                                )
                            }

                            Column(
                                modifier = if (hasAttachment) Modifier.width(IntrinsicSize.Min) else Modifier,
                                horizontalAlignment = alignment
                            ) {
                                // GIFs, attachments, etc.
                                additionalContent(onDragChange, onDrag, textContent)

                                if (downloadState != null) {
                                    DownloadIndication(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = if(data.content.isNullOrBlank()) messageShape else RectangleShape,
                                        state = downloadState
                                    )
                                }

                                Box(modifier = Modifier.animateContentSize(
                                    alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart,
                                    animationSpec = spring(stiffness = Spring.StiffnessHigh)
                                )) {
                                    // textual content
                                    when {
                                        data.state == MessageState.Decrypting -> {
                                            Box(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .requiredHeight(
                                                        20.dp + with(density) { LocalTheme.current.styles.title.fontSize.toDp() }
                                                    )
                                                    .background(
                                                        color = if (isCurrentUser) {
                                                            LocalTheme.current.colors.brandMainDark
                                                        } else LocalTheme.current.colors.backgroundContrast,
                                                        shape = messageShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LoadingMessageBubble(
                                                    key = data.id.hashCode(),
                                                    data = ConversationTypingIndicator(
                                                        authorPublicId = data.authorPublicId
                                                    )
                                                )
                                            }
                                        }
                                        !data.content.isNullOrEmpty() -> {
                                            val showReadMore = remember(data.id) {
                                                mutableStateOf(false)
                                            }
                                            val text = @Composable {
                                                Column(
                                                    modifier = Modifier
                                                        .then(
                                                            if (!data.reactions.isNullOrEmpty()) {
                                                                Modifier.padding(bottom = with(density) {
                                                                    LocalTheme.current.styles.category.fontSize.toDp() + 6.dp
                                                                })
                                                            } else Modifier
                                                        )
                                                        .background(
                                                            color = if (isCurrentUser) {
                                                                LocalTheme.current.colors.brandMainDark
                                                            } else LocalTheme.current.colors.backgroundContrast,
                                                            shape = messageShape
                                                        )
                                                        .padding(
                                                            vertical = 10.dp,
                                                            horizontal = 14.dp
                                                        )
                                                ) {
                                                    Text(
                                                        modifier = Modifier
                                                            .widthIn(max = (screenSize.width * .8f).dp)
                                                            .then(
                                                                if(hasAttachment) Modifier.fillMaxWidth() else Modifier
                                                            )
                                                            .then(if(model.transcribe.value) Modifier else Modifier),
                                                        text = textContent,
                                                        maxLines = MaximumTextLines,
                                                        overflow = TextOverflow.Ellipsis,
                                                        onTextLayout = {
                                                            showReadMore.value = it.didOverflowHeight
                                                        }
                                                    )
                                                    AnimatedVisibility(showReadMore.value) {
                                                        Text(
                                                            modifier = Modifier
                                                                .padding(top = 4.dp)
                                                                .scalingClickable {
                                                                    model.openDetail()
                                                                },
                                                            text = stringResource(Res.string.message_read_more),
                                                            style = LocalTheme.current.styles.title.copy(
                                                                fontFamily = FontFamily(fontQuicksandSemiBold)
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                            if(showOptions || isMouseUser) {
                                                SelectionContainer {
                                                    text()
                                                }
                                            }else text()
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        modifier = Modifier
                                            .align(
                                                if (isCurrentUser) Alignment.BottomStart else Alignment.BottomEnd
                                            )
                                            .zIndex(2f),
                                        visible = !data.reactions.isNullOrEmpty()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(
                                                    start = if (isCurrentUser) 0.dp else 12.dp,
                                                    end = if (isCurrentUser) 12.dp else 0.dp
                                                )
                                                .then(
                                                    if ((data.reactions?.size ?: 0) > 1) {
                                                        Modifier.offset(x = if (isCurrentUser) (-8).dp else 8.dp)
                                                    } else Modifier
                                                )
                                                .offset(
                                                    x = 0.dp,
                                                    y = with(density) {
                                                        -LocalTheme.current.styles.category.fontSize.toDp() + 10.dp
                                                    }
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            data.reactions?.take(MaximumReactions)?.forEach { reaction ->
                                                Row(
                                                    Modifier
                                                        .scalingClickable {
                                                            if (data.reactions.size > 1) {
                                                                showDetailDialogOf.value = data.content to reaction.content
                                                            }
                                                        }
                                                        .width(IntrinsicSize.Min)
                                                        .background(
                                                            color = LocalTheme.current.colors.disabledComponent,
                                                            shape = LocalTheme.current.shapes.componentShape
                                                        )
                                                        .padding(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            modifier = Modifier.padding(end = 2.dp),
                                                            text = reaction.content ?: "",
                                                            style = LocalTheme.current.styles.category.copy(
                                                                textAlign = TextAlign.Center
                                                            )
                                                        )
                                                        if (reaction.user?.userPublicId == currentUserPublicId) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .height(2.dp)
                                                                    .fillMaxWidth(.6f)
                                                                    .background(
                                                                        color = LocalTheme.current.colors.brandMain,
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    )
                                                            )
                                                        }
                                                    }
                                                    val count = remember(data.id) {
                                                        mutableStateOf(1)
                                                    }
                                                    LaunchedEffect(data.reactions) {
                                                        count.value = data.reactions.count {
                                                            it.content == reaction.content
                                                        }
                                                    }

                                                    count.value.takeIf { it > 1 }?.let {
                                                        Text(
                                                            text = it.toString(),
                                                            style = LocalTheme.current.styles.regular
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Options(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(end = if (isCurrentUser) 16.dp else 0.dp),
                                visible = showOptions,
                                hasMedia = data.media?.isEmpty() == false,
                                onDownloadRequest = onDownloadRequest,
                                onReplyRequest = { model.onReplyRequest() },
                                onReactionRequest = { model.onReactionRequest(it) }
                            )

                            // bottom spacing
                            AnimatedVisibility(isReacting) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    // desktop options
                    if(!isCurrentUser) {
                        Options(
                            modifier = Modifier
                                .padding(contentPadding)
                                .padding(start = 8.dp),
                            visible = !showOptions && isFocused.value,
                            hasMedia = data.media?.isEmpty() == false,
                            onDownloadRequest = onDownloadRequest,
                            onReplyRequest = { model.onReplyRequest() },
                            onReactionRequest = { model.onReactionRequest(it) }
                        )
                    }
                }
            }

            AnimatedVisibility(isReacting) {
                Text(
                    modifier = Modifier.padding(end = 6.dp),
                    text = (data.state?.description?.plus(", ") ?: "") +
                            " ${data.sentAt?.formatAsRelative() ?: ""}",
                    style = LocalTheme.current.styles.regular
                )
            }

            if (isCurrentUser && (isMyLastMessage || (data.state?.ordinal ?: 0) < MessageState.Sent.ordinal)) {
                data.state?.imageVector?.let { imgVector ->
                    Icon(
                        modifier = Modifier
                            .offset(y = if(isReacting) 0.dp else -contentPadding.calculateBottomPadding())
                            .zIndex(2f)
                            .size(16.dp),
                        imageVector = imgVector,
                        contentDescription = data.state.description,
                        tint = if (data.state == MessageState.Failed) {
                            SharedColors.RED_ERROR
                        } else LocalTheme.current.colors.disabled
                    )
                } ?: CircularProgressIndicator(
                    modifier = Modifier.requiredSize(12.dp),
                    color = LocalTheme.current.colors.disabled,
                    trackColor = LocalTheme.current.colors.disabledComponent,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun Options(
    modifier: Modifier = Modifier,
    visible: Boolean,
    hasMedia: Boolean,
    onDownloadRequest: () -> Unit,
    onReplyRequest: () -> Unit,
    onReactionRequest: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val buttonSize = with(density) { LocalTheme.current.styles.heading.fontSize.toDp() } + 2.dp

    AnimatedVisibility(
        visible = visible,
    ) {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            /*Icon(
                modifier = Modifier
                    .scalingClickable {
                        //onForwardRequest()
                    }
                    .padding(5.dp),
                painter = painterResource(Res.drawable.ic_forward),
                contentDescription = stringResource(Res.string.accessibility_message_forward),
                tint = LocalTheme.current.colors.secondary
            )*/
            if (hasMedia) {
                Icon(
                    modifier = Modifier
                        .scalingClickable { onDownloadRequest() }
                        .size(buttonSize)
                        .padding(2.dp),
                    imageVector = Icons.Outlined.Download,
                    contentDescription = stringResource(Res.string.accessibility_message_download),
                    tint = LocalTheme.current.colors.secondary
                )
            }
            Icon(
                modifier = Modifier
                    .scalingClickable { onReplyRequest() }
                    .size(buttonSize)
                    .padding(2.dp),
                imageVector = Icons.AutoMirrored.Outlined.Reply,
                contentDescription = stringResource(Res.string.accessibility_message_reply),
                tint = LocalTheme.current.colors.secondary
            )
            Icon(
                modifier = Modifier
                    .scalingClickable { onReactionRequest(true) }
                    .size(buttonSize)
                    .padding(2.dp),
                imageVector = Icons.Outlined.Mood,
                contentDescription = stringResource(Res.string.accessibility_action_message_react),
                tint = LocalTheme.current.colors.secondary
            )
        }
    }
}

@Composable
private fun ShimmerLayout(modifier: Modifier = Modifier) {
    val randomFraction = remember { (3..7).random() / 10f }
    Box(
        modifier = modifier
            .brandShimmerEffect(shape = LocalTheme.current.shapes.circularActionShape)
            .padding(
                vertical = 10.dp,
                horizontal = 12.dp
            )
            .fillMaxWidth(randomFraction)
    ) {
        Text(
            text = "",
            style = LocalTheme.current.styles.category
        )
    }
}

// maximum visible reactions within message bubble
const val MaximumReactions = 4
private const val MaximumTextLines = 6
private const val DragCancelDelayMillis = 100L
