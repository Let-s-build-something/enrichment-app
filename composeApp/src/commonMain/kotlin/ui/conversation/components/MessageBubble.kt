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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mood
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_message_react
import augmy.composeapp.generated.resources.accessibility_message_reply
import augmy.composeapp.generated.resources.accessibility_reaction_other
import augmy.interactive.shared.DateUtils.formatAsRelative
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.detectMessageInteraction
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import base.utils.tagToColor
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.EmojiData
import data.io.user.NetworkItemIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue

@Composable
fun rememberMessageBubbleState(
    onReactionRequest: (Boolean) -> Unit,
    onReactionChange: (String) -> Unit,
    onAdditionalReactionRequest: () -> Unit,
    onReplyRequest: () -> Unit
): MessageBubbleState {
    return remember {
        MessageBubbleState(
            onReactionRequest = onReactionRequest,
            onReactionChange = onReactionChange,
            onAdditionalReactionRequest = onAdditionalReactionRequest,
            onReplyRequest = onReplyRequest
        )
    }
}

data class MessageBubbleState(
    val onReactionRequest: (Boolean) -> Unit,
    val onReactionChange: (String) -> Unit,
    val onAdditionalReactionRequest: () -> Unit,
    val onReplyRequest: () -> Unit
)

/** Horizontal bubble displaying textual content of a message and its reactions */
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    data: ConversationMessageIO?,
    users: List<NetworkItemIO>,
    isReacting: Boolean,
    preferredEmojis: List<EmojiData>,
    enabled: Boolean = true,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    isReplying: Boolean = false,
    currentUserPublicId: String,
    state: MessageBubbleState,
    additionalContent: @Composable ColumnScope.() -> Unit
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
                enabled = enabled,
                isReacting = isReacting,
                isReplying = isReplying,
                state = state,
                additionalContent = additionalContent
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
    hasNext: Boolean,
    enabled: Boolean,
    isReplying: Boolean = false,
    currentUserPublicId: String,
    isReacting: Boolean,
    state: MessageBubbleState,
    additionalContent: @Composable ColumnScope.() -> Unit
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
    val replyIndicationSize = with(density) { LocalTheme.current.styles.category.fontSize.toDp() + 20.dp }
    val hoverInteractionSource = remember { MutableInteractionSource() }

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
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(isCurrentUser && isCompact && isReacting && !isReplying) {
            Icon(
                modifier = Modifier
                    .scalingClickable { state.onReplyRequest() }
                    .padding(5.dp),
                imageVector = Icons.AutoMirrored.Outlined.Reply,
                contentDescription = stringResource(Res.string.accessibility_message_reply),
                tint = LocalTheme.current.colors.secondary
            )
        }

        Column(
            modifier = Modifier
                .hoverable(
                    enabled = !isCompact,
                    interactionSource = hoverInteractionSource
                )
                .padding(vertical = verticalPadding.value.dp)
                .offset(
                    x = with(density) { animatedOffsetX.value.toDp() } + additionalOffsetDp.value.dp
                )
                .pointerInput(enabled) {
                    detectMessageInteraction(
                        onTap = {
                            showHistory.value = !showHistory.value
                            state.onReactionRequest(false)
                        },
                        onLongPress = {
                            state.onReactionRequest(true)
                        },
                        onDrag = { dragged ->
                            isDragged.value = dragged

                            // cancel dragging and animate back to original position
                            dragCoroutineScope.coroutineContext.cancelChildren()
                            if(!dragged) {
                                if(animatedOffsetX.value !in replyBounds) {
                                    coroutineScope.launch {
                                        state.onReplyRequest()
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
                }
                .then(
                    if(isReacting) {
                        Modifier
                            .background(
                                color = LocalTheme.current.colors.component,
                                shape = LocalTheme.current.shapes.componentShape
                            )
                    }else Modifier
                )
                .animateContentSize(),
            horizontalAlignment = if(isCurrentUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // new or a change of a reaction - indication
            AnimatedVisibility(isReacting) {
                Row(
                    modifier = modifier
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
                                    state.onReactionChange(emojiData.emoji.firstOrNull() ?: "")
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
                                state.onAdditionalReactionRequest()
                            },
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(Res.string.accessibility_reaction_other),
                        tint = LocalTheme.current.colors.secondary
                    )
                }
            }

            additionalContent()

            // message content + reply function + desktop options
            Row(modifier = modifier) {
                if(!isCompact && isCurrentUser) {
                    val isFocused = hoverInteractionSource.collectIsHoveredAsState()

                    LaunchedEffect(isFocused) {
                        if(isFocused.value) {
                            showHistory.value = true
                        }
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.Top),
                        visible = isFocused.value
                    ) {
                        DesktopOptions(
                            modifier = Modifier.padding(top = 5.dp, start = 8.dp),
                            onReaction = { state.onReactionRequest(!isReacting) },
                            onReply = { state.onReplyRequest() }
                        )
                    }
                }


                // message content + reply function + reactions + desktop options
                Box(contentAlignment = Alignment.CenterEnd) {
                    // message content + reply function
                    Box {
                        if(animatedOffsetX.value.absoluteValue > 0f || isReplying) {
                            val percentageAchieved = (if(isCurrentUser) {
                                animatedOffsetX.value / replyBounds.start
                            }else animatedOffsetX.value / replyBounds.endInclusive).times(2)

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (if(isCurrentUser) replyIndicationSize + 4.dp else -replyIndicationSize - 4.dp).times(
                                            if(isReplying) 1f else percentageAchieved.coerceAtMost(1f)
                                        )
                                    )
                                    .align(if(isCurrentUser) Alignment.TopEnd else Alignment.TopStart)
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
                                        .then(if(animatedOffsetX.value !in replyBounds) {
                                            Modifier.background(
                                                color = LocalTheme.current.colors.component,
                                                shape = CircleShape
                                            )
                                        }else Modifier)
                                        .padding(5.dp),
                                    imageVector = Icons.AutoMirrored.Outlined.Reply,
                                    contentDescription = stringResource(Res.string.accessibility_message_reply),
                                    tint = LocalTheme.current.colors.secondary
                                )
                            }
                        }

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
                                    color = tagToColor(data.user?.tag) ?: if(isCurrentUser) {
                                        LocalTheme.current.colors.brandMainDark
                                    } else LocalTheme.current.colors.backgroundContrast,
                                    shape = if(isCurrentUser) {
                                        RoundedCornerShape(
                                            topStart = 24.dp,
                                            bottomStart = 24.dp,
                                            topEnd = if(hasPrevious) 1.dp else 24.dp,
                                            bottomEnd = if(hasNext) 1.dp else 24.dp
                                        )
                                    }else {
                                        RoundedCornerShape(
                                            topEnd = 24.dp,
                                            bottomEnd = 24.dp,
                                            topStart = if(hasPrevious) 1.dp else 24.dp,
                                            bottomStart = if(hasNext) 1.dp else 24.dp
                                        )
                                    }
                                )
                                .padding(
                                    vertical = 10.dp,
                                    horizontal = 14.dp
                                )
                                .animateContentSize()
                        ) {
                            Text(
                                modifier = Modifier,
                                text = data.content ?: "",
                                style = LocalTheme.current.styles.category.copy(
                                    color = if(isCurrentUser) Colors.GrayLight else LocalTheme.current.colors.secondary
                                )
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.align(
                            if(isCurrentUser) Alignment.BottomStart else Alignment.BottomEnd
                        ),
                        visible = !data.reactions.isNullOrEmpty()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    start = if(isCurrentUser) 0.dp else 12.dp,
                                    end = if(isCurrentUser) 12.dp else 0.dp,
                                    top = with(density) {
                                        LocalTheme.current.styles.category.fontSize.toDp() + 6.dp
                                    }
                                )
                                .then(
                                    if(reactions.value.size > 1) {
                                        Modifier.offset(x = if(isCurrentUser) (-8).dp else 8.dp)
                                    }else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            reactions.value.take(MaximumReactions).forEach { reaction ->
                                Row(
                                    Modifier
                                        .scalingClickable {
                                            if((data.reactions?.size ?: 0) > 1) {
                                                showDetailDialogOf.value = data.content to reaction.first
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
                                        if(reaction.second.second) {
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


                // desktop reactions for messages of others
                if(!isCompact && !isCurrentUser) {
                    val isFocused = hoverInteractionSource.collectIsHoveredAsState()
                    LaunchedEffect(isFocused) {
                        if(isFocused.value) {
                            showHistory.value = true
                        }
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.Top),
                        visible = isFocused.value || isReacting
                    ) {
                        DesktopOptions(
                            modifier = Modifier.padding(top = 5.dp, end = 8.dp),
                            onReaction = { state.onReactionRequest(!isReacting) },
                            onReply = { state.onReplyRequest() }
                        )
                    }
                }
            }

            AnimatedVisibility(showHistory.value) {
                Text(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                    text = data.createdAt?.formatAsRelative() ?: "",
                    style = LocalTheme.current.styles.regular
                )
            }

            // bottom spacing
            AnimatedVisibility(isReacting) {
                Spacer(Modifier.height(8.dp))
            }
        }

        AnimatedVisibility(!isCurrentUser && isCompact && isReacting && !isReplying) {
            Icon(
                modifier = Modifier
                    .scalingClickable { state.onReplyRequest() }
                    .padding(5.dp),
                imageVector = Icons.AutoMirrored.Outlined.Reply,
                contentDescription = stringResource(Res.string.accessibility_message_reply),
                tint = LocalTheme.current.colors.secondary
            )
        }
    }
}

@Composable
private fun DesktopOptions(
    modifier: Modifier = Modifier,
    onReply: () -> Unit,
    onReaction: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier
                .scalingClickable {
                    onReply()
                }
                .padding(5.dp),
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = stringResource(Res.string.accessibility_message_reply),
            tint = LocalTheme.current.colors.secondary
        )
        Icon(
            modifier = Modifier
                .scalingClickable {
                    onReaction()
                }
                .padding(5.dp),
            imageVector = Icons.Outlined.Mood,
            contentDescription = stringResource(Res.string.accessibility_action_message_react),
            tint = LocalTheme.current.colors.secondary
        )
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
