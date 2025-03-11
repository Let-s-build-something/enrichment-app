package ui.conversation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CrueltyFree
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_action_emojis
import augmy.composeapp.generated.resources.accessibility_action_gifs
import augmy.composeapp.generated.resources.accessibility_action_stickers
import augmy.interactive.shared.ext.mouseDraggable
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.network.conversation.giphy.GifAsset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.conversation.ConversationModel
import ui.conversation.components.emoji.MessageEmojiPanel
import ui.conversation.components.gif.MessageGifPanel

/** A conversation panel for selecting media for messages */
@Composable
fun MessageMediaPanel(
    modifier: Modifier = Modifier,
    mode: MutableState<Int>,
    viewModel: ConversationModel,
    showBackSpace: Boolean,
    onGifSelected: (GifAsset) -> Unit,
    onEmojiSelected: (String) -> Unit,
    onBackSpace: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val density = LocalDensity.current
    val keyboardHeight = viewModel.keyboardHeight.collectAsState()
    val imePadding = WindowInsets.ime.getBottom(density)
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = ConversationKeyboardMode.Emoji.ordinal,
        pageCount = { ConversationKeyboardMode.entries.size - 1 }
    )

    val isFilterFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isFilterFocused.value, imePadding) {
        if(isFilterFocused.value && imePadding > 0) {
            viewModel.additionalBottomPadding.animateTo(with(density) { 100.dp.toPx() })
        }else {
            viewModel.additionalBottomPadding.animateTo(0f)
        }
    }

    LaunchedEffect(imePadding, isFilterFocused.value) {
        delay(100)
        if(keyboardHeight.value - imePadding <= 0 && isFilterFocused.value.not()) {
            onDismissRequest()
        }
    }

    LaunchedEffect(mode.value) {
        pagerState.animateScrollToPage(mode.value)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect {
            mode.value = ConversationKeyboardMode.entries[it].ordinal
        }
    }

    Row(
        modifier = modifier
            .padding(
                bottom = with(density) {
                    viewModel.additionalBottomPadding.value.toDp()
                }
            )
            .then(
                if(mode.value == ConversationKeyboardMode.Default.ordinal) Modifier.height(0.dp)
                else if (isFilterFocused.value && mode.value == ConversationKeyboardMode.Gif.ordinal) {
                    Modifier.fillMaxHeight(.75f)
                } else Modifier.height(with(density) { keyboardHeight.value.toDp() })
            )
            .animateContentSize()
    ) {
        HorizontalPager(
            modifier = Modifier
                .mouseDraggable(pagerState) {
                    mode.value = ConversationKeyboardMode.entries[it].ordinal
                }
                .weight(1f)
                .animateContentSize(),
            state = pagerState,
            beyondViewportPageCount = 0
        ) { index ->
            when(index) {
                ConversationKeyboardMode.Emoji.ordinal -> MessageEmojiPanel(
                    viewModel = viewModel,
                    showBackSpace = showBackSpace,
                    isFilterFocused = isFilterFocused,
                    onEmojiSelected = onEmojiSelected,
                    onBackSpace = onBackSpace
                )
                ConversationKeyboardMode.Gif.ordinal -> MessageGifPanel(
                    viewModel = viewModel,
                    isFilterFocused = isFilterFocused,
                    onGifSelected = onGifSelected
                )
                ConversationKeyboardMode.Stickers.ordinal -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Awaiting emoji.gg API v2 completion",
                        modifier = Modifier.padding(16.dp),
                        style = LocalTheme.current.styles.heading
                    )
                }
            }
        }

        // buttons for switching the mode
        Column(
            modifier = Modifier
                .clickable(indication = null, interactionSource = null, onClick = {})
                .padding(end = 6.dp, bottom = 24.dp)
                .fillMaxHeight()
                .wrapContentWidth()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConversationKeyboardMode.entries.forEach { entry ->
                if(entry != ConversationKeyboardMode.Default) {
                    Icon(
                        modifier = Modifier
                            .scalingClickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(entry.ordinal)
                                }
                            }
                            .size(32.dp)
                            .background(
                                color = LocalTheme.current.colors.backgroundLight,
                                shape = LocalTheme.current.shapes.rectangularActionShape
                            )
                            .padding(4.dp),
                        imageVector = when(entry) {
                            ConversationKeyboardMode.Gif -> Icons.Outlined.Gif
                            ConversationKeyboardMode.Stickers -> Icons.Outlined.CrueltyFree
                            else -> Icons.Outlined.Mood
                        },
                        contentDescription = stringResource(
                            when(entry) {
                                ConversationKeyboardMode.Gif -> Res.string.accessibility_action_gifs
                                ConversationKeyboardMode.Stickers -> Res.string.accessibility_action_stickers
                                else -> Res.string.accessibility_action_emojis
                            }
                        ),
                        tint = if(pagerState.currentPage == entry.ordinal) {
                            LocalTheme.current.colors.disabled
                        } else LocalTheme.current.colors.primary
                    )
                }
            }
        }
    }
}
