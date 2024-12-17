package ui.conversation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ext.mouseDraggable
import augmy.interactive.shared.ui.theme.LocalTheme
import data.io.social.network.conversation.giphy.GifAsset
import kotlinx.coroutines.delay
import ui.conversation.ConversationViewModel
import ui.conversation.components.emoji.MessageEmojiPanel
import ui.conversation.components.gif.MessageGifPanel

/** A conversation panel for selecting media for messages */
@Composable
fun MessageMediaPanel(
    modifier: Modifier = Modifier,
    mode: MutableState<Int>,
    viewModel: ConversationViewModel,
    onGifSelected: (GifAsset) -> Unit,
    onEmojiSelected: (String) -> Unit,
    onBackSpace: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val density = LocalDensity.current
    val imeHeight = viewModel.keyboardHeight
    val imePadding = WindowInsets.ime.getBottom(density)

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
        if(imeHeight - imePadding <= 0 && isFilterFocused.value.not()) {
            onDismissRequest()
        }
    }

    LaunchedEffect(mode.value) {
        pagerState.animateScrollToPage(mode.value)
    }

    HorizontalPager(
        modifier = modifier
            .mouseDraggable(pagerState) {
                mode.value = ConversationKeyboardMode.entries[it].ordinal
            }
            .padding(
                bottom = with(density) {
                    viewModel.additionalBottomPadding.value.toDp()
                }
            )
            .then(
                if(mode.value == ConversationKeyboardMode.Default.ordinal) Modifier.height(0.dp)
                else if (isFilterFocused.value && mode.value == ConversationKeyboardMode.Gif.ordinal) {
                    Modifier.fillMaxHeight(.75f)
                } else Modifier.height(with(density) { imeHeight.toDp() })
            )
            .fillMaxWidth()
            .animateContentSize(),
        state = pagerState,
        beyondViewportPageCount = 0
    ) { index ->
        when(index) {
            ConversationKeyboardMode.Emoji.ordinal -> MessageEmojiPanel(
                viewModel = viewModel,
                visible = true,
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
                    text = "Stickers",
                    modifier = Modifier.padding(16.dp),
                    style = LocalTheme.current.styles.heading
                )
            }
        }
    }
}
