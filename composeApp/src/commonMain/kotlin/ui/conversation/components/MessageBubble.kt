package ui.conversation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_message_react
import augmy.composeapp.generated.resources.accessibility_message_download
import augmy.composeapp.generated.resources.accessibility_message_reply
import augmy.composeapp.generated.resources.accessibility_reaction_other
import augmy.composeapp.generated.resources.action_settings
import augmy.interactive.shared.DateUtils.formatAsRelative
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.detectMessageInteraction
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.theme.Colors
import base.utils.openLink
import base.utils.tagToColor
import components.buildAnnotatedLinkString
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.EmojiData
import data.io.social.network.conversation.MessageState
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.components.audio.MediaProcessorModel
import ui.conversation.media.DownloadIndication
import ui.conversation.media.rememberIndicationState
import kotlin.math.absoluteValue


/**
 * Horizontal bubble displaying textual content of a message and its reactions
 * @param isMyLastMessage whether this message is the last of the current user overall
 */
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO?,
    users: List<NetworkItemIO>,
    isReacting: Boolean,
    preferredEmojis: List<EmojiData>,
    hasPrevious: Boolean,
    hasNext: Boolean,
    isMyLastMessage: Boolean,
    isReplying: Boolean,
    currentUserPublicId: String,
    onReactionRequest: (Boolean) -> Unit,
    onReactionChange: (String) -> Unit,
    onAdditionalReactionRequest: () -> Unit,
    onReplyRequest: () -> Unit,
    additionalContent: @Composable () -> Unit
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
                users = users,
                preferredEmojis = preferredEmojis,
                currentUserPublicId = currentUserPublicId,
                isReacting = isReacting,
                isReplying = isReplying,
                isMyLastMessage = isMyLastMessage,
                additionalContent = additionalContent,
                onReactionRequest = onReactionRequest,
                onReactionChange = onReactionChange,
                onAdditionalReactionRequest = onAdditionalReactionRequest,
                onReplyRequest = onReplyRequest
            )
        }
    }
}

@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO,
    users: List<NetworkItemIO>,
    preferredEmojis: List<EmojiData>,
    hasPrevious: Boolean,
    isMyLastMessage: Boolean,
    hasNext: Boolean,
    isReplying: Boolean,
    currentUserPublicId: String,
    isReacting: Boolean,
    onReactionRequest: (Boolean) -> Unit,
    onReactionChange: (String) -> Unit,
    onAdditionalReactionRequest: () -> Unit,
    onReplyRequest: () -> Unit,
    additionalContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val screenSize = LocalScreenSize.current
    val isCompact = LocalDeviceType.current == WindowWidthSizeClass.Compact
    val coroutineScope = rememberCoroutineScope()
    val dragCoroutineScope = rememberCoroutineScope()
    val isCurrentUser = data.authorPublicId == currentUserPublicId
    val replyBounds = remember {
        with(density) {
            (-screenSize.width.dp.toPx() / 8f)..(screenSize.width.dp.toPx() / 8f)
        }
    }
    val buttonSize = with(density) { LocalTheme.current.styles.heading.fontSize.toDp() } + 6.dp
    val replyIndicationSize = with(density) { LocalTheme.current.styles.category.fontSize.toDp() + 20.dp }
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val processor = if(data.mediaUrls?.isEmpty() == false) koinViewModel<MediaProcessorModel>(key = data.id) else null
    val downloadState = if(processor != null) rememberIndicationState(processor) else null

    val reactions = remember(data.id) {
        mutableStateOf(listOf<Pair<String?, Pair<List<NetworkItemIO>, Boolean>>>())
    }
    val showDetailDialogOf = remember(data.id) {
        mutableStateOf<Pair<String?, String?>?>(null)
    }
    val isDragged = remember(data.id) {
        mutableStateOf(false)
    }
    val showHistory = remember(data.id) {
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
        processor?.processFiles(
            *data.mediaUrls.orEmpty().toTypedArray()
        )
    }

    LaunchedEffect(Unit, data.reactions) {
        withContext(Dispatchers.Default) {
            val map = hashMapOf<String?, Pair<List<NetworkItemIO>, Boolean>>()
            data.reactions?.forEach { reaction ->
                map[reaction.content] = Pair(
                    users.find { it.publicId == reaction.authorPublicId }?.let {
                        map[reaction.content]?.first.orEmpty().plus(it)
                    } ?: map[reaction.content]?.first.orEmpty(),
                    (map[reaction.content]?.second ?: false) || (reaction.authorPublicId == currentUserPublicId)
                )
            }
            reactions.value = map.toList().sortedByDescending { it.second.first.size }
        }
    }

    showDetailDialogOf.value?.let {
        MessageReactionsDialog(
            reactions = reactions,
            users = users,
            messageContent = it.first,
            initialEmojiSelection = it.second,
            reactionsRaw = data.reactions.orEmpty(),
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
                        if(isReacting) onReactionRequest(false)
                        else showHistory.value = !showHistory.value
                    },
                    onLongPress = {
                        onReactionRequest(true)
                    },
                    onDrag = { dragged ->
                        isDragged.value = dragged

                        // cancel dragging and animate back to original position
                        dragCoroutineScope.coroutineContext.cancelChildren()
                        if(!dragged) {
                            if(animatedOffsetX.value !in replyBounds) {
                                coroutineScope.launch {
                                    onReplyRequest()
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
                    },
                    onDragChange = { _, dragAmount ->
                        offsetX.value = (offsetX.value + dragAmount.x / 3).coerceIn(
                            minimumValue = if(isCurrentUser) replyBounds.start.times(1.4f) else 0f,
                            maximumValue = if(isCurrentUser) 0f else replyBounds.endInclusive.times(1.4f)
                        )
                        coroutineScope.launch {
                            animatedOffsetX.animateTo(offsetX.value)
                        }
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // message content + reply function + reactions
            Box {
                // message content + reply function
                Box(
                    if (isReacting || data.anchorMessage != null) {
                        Modifier.background(
                            color = LocalTheme.current.colors.backgroundDark,
                            shape = LocalTheme.current.shapes.componentShape
                        )
                    } else Modifier
                ) {
                    if (animatedOffsetX.value.absoluteValue > 0f || isReplying) {
                        val percentageAchieved = (if (isCurrentUser) {
                            animatedOffsetX.value / replyBounds.start
                        } else animatedOffsetX.value / replyBounds.endInclusive).times(2)

                        Box(
                            modifier = Modifier
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

                    Column(
                        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
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
                                                onReactionChange(emojiData.emoji.firstOrNull() ?: "")
                                            }
                                            .padding(8.dp),
                                        text = emojiData.emoji.firstOrNull() ?: "",
                                        style = LocalTheme.current.styles.heading
                                    )
                                }
                                Icon(
                                    modifier = Modifier
                                        .size(buttonSize)
                                        .scalingClickable {
                                            onAdditionalReactionRequest()
                                        },
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = stringResource(Res.string.accessibility_reaction_other),
                                    tint = LocalTheme.current.colors.secondary
                                )
                            }
                        }

                        val messageShape = if (isCurrentUser) {
                            RoundedCornerShape(
                                topStart = if(data.mediaUrls?.isEmpty() == false) 1.dp else 24.dp,
                                topEnd = if(hasPrevious || !data.mediaUrls.isNullOrEmpty()) 1.dp else 24.dp,
                                bottomStart = 24.dp,
                                bottomEnd = if (hasNext) 1.dp else 24.dp
                            )
                        } else {
                            RoundedCornerShape(
                                topEnd = if(data.mediaUrls?.isEmpty() == false) 1.dp else 24.dp,
                                topStart = if(hasPrevious || !data.mediaUrls.isNullOrEmpty()) 1.dp else 24.dp,
                                bottomEnd = 24.dp,
                                bottomStart = if (hasNext) 1.dp else 24.dp
                            )
                        }

                        Column(modifier = if (data.mediaUrls?.isEmpty() == false) Modifier.width(IntrinsicSize.Min) else Modifier) {
                            // GIFs, attachments, etc.
                            additionalContent()

                            if (downloadState != null) {
                                DownloadIndication(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = if(data.content.isNullOrBlank()) messageShape else RectangleShape,
                                    state = downloadState
                                )
                            }

                            // textual content
                            if (!data.content.isNullOrEmpty()) {
                                Text(
                                    modifier = Modifier
                                        .widthIn(max = (screenSize.width * .8f).dp)
                                        .then(
                                            if(data.mediaUrls?.isEmpty() == false) Modifier.fillMaxWidth() else Modifier
                                        )
                                        .then(
                                            if (!data.reactions.isNullOrEmpty()) {
                                                Modifier.padding(bottom = with(density) {
                                                    LocalTheme.current.styles.category.fontSize.toDp() + 6.dp
                                                })
                                            } else Modifier
                                        )
                                        .background(
                                            color = tagToColor(data.user?.tag) ?: if (isCurrentUser) {
                                                LocalTheme.current.colors.brandMainDark
                                            } else LocalTheme.current.colors.backgroundContrast,
                                            shape = messageShape
                                        )
                                        .padding(
                                            vertical = 10.dp,
                                            horizontal = 14.dp
                                        ),
                                    text = buildAnnotatedLinkString(
                                        text = data.content,
                                        onLinkClicked = { openLink(it) }
                                    ),
                                    style = LocalTheme.current.styles.category.copy(
                                        color = if (isCurrentUser) Colors.GrayLight else LocalTheme.current.colors.secondary
                                    )
                                )
                            }
                        }

                        val isFocused = hoverInteractionSource.collectIsHoveredAsState()

                        LaunchedEffect(isFocused) {
                            if (isFocused.value) {
                                showHistory.value = true
                            }
                        }

                        AnimatedVisibility(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = if (isCurrentUser) 16.dp else 0.dp),
                            visible = (isReacting && !isReplying) || (isFocused.value)
                        ) {
                            val showOptions = remember(data.id, isFocused.value) {
                                mutableStateOf(isFocused.value)
                            }

                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .animateContentSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (showOptions.value) {
                                    /*Icon(
                                        modifier = Modifier
                                            .scalingClickable {
                                                onForwardRequest()
                                            }
                                            .padding(5.dp),
                                        painter = painterResource(Res.drawable.ic_forward),
                                        contentDescription = stringResource(Res.string.accessibility_message_forward),
                                        tint = LocalTheme.current.colors.secondary
                                    )*/
                                    if (data.mediaUrls?.isEmpty() == false) {
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
                                if(!showOptions.value && currentPlatform != PlatformType.Jvm) {
                                    Icon(
                                        modifier = Modifier
                                            .scalingClickable { showOptions.value = true }
                                            .size(buttonSize)
                                            .padding(2.dp),
                                        imageVector = Icons.Outlined.MoreHoriz,
                                        contentDescription = stringResource(Res.string.action_settings),
                                        tint = LocalTheme.current.colors.secondary
                                    )
                                }
                            }
                        }

                        // bottom spacing
                        AnimatedVisibility(isReacting) {
                            Spacer(Modifier.height(8.dp))
                        }
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
                                if (reactions.value.size > 1) {
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
                        reactions.value.take(MaximumReactions).forEach { reaction ->
                            Row(
                                Modifier
                                    .scalingClickable {
                                        if ((data.reactions?.size ?: 0) > 1) {
                                            showDetailDialogOf.value =
                                                data.content to reaction.first
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
                                        text = reaction.first ?: "",
                                        style = LocalTheme.current.styles.category.copy(
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    if (reaction.second.second) {
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
                                reaction.second.first.size.takeIf { it > 1 }?.let { count ->
                                    Text(
                                        text = count.toString(),
                                        style = LocalTheme.current.styles.regular
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .zIndex(2f)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentUser && (isMyLastMessage || (data.state?.ordinal
                        ?: 0) < MessageState.Sent.ordinal)
                ) {
                    data.state?.imageVector?.let { imgVector ->
                        Icon(
                            modifier = Modifier.size(16.dp),
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

                if (showHistory.value) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 6.dp),
                        text = "${data.state?.description?.plus(", ") ?: ""}${data.createdAt?.formatAsRelative() ?: ""}",
                        style = LocalTheme.current.styles.regular
                    )
                }
            }
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
private const val MaximumReactions = 4
private const val DragCancelDelayMillis = 100L
