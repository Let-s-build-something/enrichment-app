package ui.conversation.message

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_play
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils.formatAsRelative
import base.BrandBaseScreen
import base.navigation.NavIconType
import base.theme.Colors
import base.theme.DefaultThemeStyles.Companion.fontQuicksandMedium
import base.utils.openLink
import components.UserProfileImage
import components.buildAnnotatedLinkString
import data.io.base.AppPingType
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageState
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ui.conversation.ConversationComponent
import ui.conversation.components.experimental.pacing.buildTempoString
import ui.conversation.components.message.MaximumReactions
import ui.conversation.components.message.MessageReactionsDialog

/** Number of network items within one screen to be shimmered */
private const val REPLIES_SHIMMER_ITEM_COUNT = 4

/** Screen of a specific message and its subsequent reactions */
@Composable
fun MessageDetailScreen(
    messageId: String?,
    conversationId: String?,
    title: String?
) {
    val viewModel: MessageDetailModel = koinViewModel(
        key = messageId,
        parameters = {
            parametersOf(messageId ?: "", conversationId ?: "")
        }
    )

    val replies = viewModel.replies.collectAsLazyPagingItems()
    val message = viewModel.message.collectAsState(null)

    val transcribing = rememberSaveable(messageId) {
        mutableStateOf(false)
    }
    val showDetailDialogOf = remember {
        mutableStateOf<Pair<String?, String?>?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.pingStream.collectLatest { stream ->
            stream.forEach {
                if(it.type == AppPingType.Conversation) {
                    if(it.identifier == messageId) {
                        replies.refresh()
                        viewModel.consumePing(messageId)
                    }else if(it.identifier == conversationId) {
                        replies.refresh()
                        viewModel.consumePing(conversationId)
                    }
                }
            }
        }
    }

    showDetailDialogOf.value?.let {
        MessageReactionsDialog(
            reactions = message.value?.reactions.orEmpty(),
            messageContent = it.first,
            initialEmojiSelection = it.second,
            onDismissRequest = {
                showDetailDialogOf.value = null
            }
        )
    }
    
    BrandBaseScreen(
        navIconType = NavIconType.CLOSE,
        title = title,
        subtitle = message.value?.sentAt?.formatAsRelative()
    ) {
        ConversationComponent(
            modifier = Modifier.fillMaxSize(),
            listModifier = Modifier
                .padding(
                    horizontal = if(LocalDeviceType.current == WindowWidthSizeClass.Compact) 0.dp else 16.dp
                )
                .fillMaxSize(),
            thread = message.value,
            verticalArrangement = Arrangement.Top,
            shimmerItemCount = REPLIES_SHIMMER_ITEM_COUNT,
            conversationId = conversationId,
            model = viewModel,
            messages = replies,
            emptyLayout = {
                // TODO
            },
            lazyScope = {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = LocalTheme.current.colors.backgroundDark,
                                shape = RoundedCornerShape(
                                    bottomEnd = LocalTheme.current.shapes.screenCornerRadius,
                                    bottomStart = LocalTheme.current.shapes.screenCornerRadius
                                )
                            )
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                            .align(Alignment.TopStart)
                    ) {
                        val isCurrentUser = viewModel.matrixUserId == message.value?.authorPublicId

                        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))
                        if(!isCurrentUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserProfileImage(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .size(48.dp),
                                        media = MediaIO(url = message.value?.user?.content?.avatarUrl),
                                        tag = null,//message.value?.user?.tag,
                                        animate = true,
                                        name = message.value?.user?.content?.displayName
                                    )
                                    Spacer(Modifier.width(LocalTheme.current.shapes.betweenItemsSpace))
                                    Text(
                                        text = message.value?.user?.content?.displayName ?: "",
                                        style = LocalTheme.current.styles.title
                                    )
                                }
                                if(!message.value?.timings.isNullOrEmpty()) {
                                    Crossfade(
                                        modifier = Modifier.padding(start = 8.dp),
                                        targetState = transcribing.value
                                    ) { isTranscribing ->
                                        Icon(
                                            modifier = Modifier
                                                .scalingClickable {
                                                    transcribing.value = !transcribing.value
                                                }
                                                .size(42.dp)
                                                .padding(4.dp),
                                            imageVector = if(isTranscribing) {
                                                Icons.Outlined.Stop
                                            }else Icons.Outlined.PlayArrow,
                                            tint = LocalTheme.current.colors.secondary,
                                            contentDescription = stringResource(Res.string.accessibility_play)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))
                        SelectionContainer {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                text = buildTempoString(
                                    enabled = transcribing.value,
                                    text = buildAnnotatedLinkString(
                                        text = message.value?.content ?: "",
                                        onLinkClicked = { openLink(it) }
                                    ),
                                    style = LocalTheme.current.styles.title.copy(
                                        color = (if (isCurrentUser) Colors.GrayLight else LocalTheme.current.colors.secondary),
                                        fontFamily = FontFamily(fontQuicksandMedium)
                                    ).toSpanStyle(),
                                    timings = message.value?.timings.orEmpty(),
                                    onFinish = {
                                        transcribing.value = false
                                    }
                                )
                            )
                        }

                        Spacer(Modifier.height(LocalTheme.current.shapes.betweenItemsSpace))

                        val reactionsRow = @Composable {
                            androidx.compose.animation.AnimatedVisibility(
                                !message.value?.reactions.isNullOrEmpty()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            start = if (isCurrentUser) 0.dp else 12.dp,
                                            end = if (isCurrentUser) 12.dp else 0.dp
                                        )
                                        .then(
                                            if ((message.value?.reactions?.size ?: 0) > 1) {
                                                Modifier.offset(x = if (isCurrentUser) (-8).dp else 8.dp)
                                            } else Modifier
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    message.value?.reactions?.take(MaximumReactions)?.forEach { reaction ->
                                        Row(
                                            Modifier
                                                .scalingClickable {
                                                    if ((message.value?.reactions?.size ?: 0) > 1) {
                                                        showDetailDialogOf.value = message.value?.content to reaction.content
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
                                                if (reaction.user?.userId == viewModel.matrixUserId) {
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
                                            val count = remember {
                                                mutableStateOf(1)
                                            }
                                            LaunchedEffect(message.value?.reactions) {
                                                count.value = message.value?.reactions?.count {
                                                    it.content == reaction.content
                                                } ?: 1
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if(isCurrentUser) Arrangement.End else Arrangement.Start
                        ) {
                            if(isCurrentUser) {
                                reactionsRow()
                            }
                            message.value?.state?.imageVector?.let { imgVector ->
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = imgVector,
                                    contentDescription = message.value?.state?.description,
                                    tint = if (message.value?.state == MessageState.Failed) {
                                        SharedColors.RED_ERROR
                                    } else LocalTheme.current.colors.disabled
                                )
                            } ?: CircularProgressIndicator(
                                modifier = Modifier.requiredSize(12.dp),
                                color = LocalTheme.current.colors.disabled,
                                trackColor = LocalTheme.current.colors.disabledComponent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = "${message.value?.state?.description ?: ""} ${message.value?.sentAt?.formatAsRelative()}",
                                style = LocalTheme.current.styles.regular
                            )
                            if(!isCurrentUser) {
                                reactionsRow()
                            }
                        }
                    }
                }
            }
        )
    }
}
