package ui.conversation.components.emoji

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_backspace
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import org.jetbrains.compose.resources.stringResource
import ui.conversation.ConversationModel

/** Panel for picking and removing emojis in a message */
@Composable
fun MessageEmojiPanel(
    modifier: Modifier = Modifier,
    viewModel: ConversationModel,
    showBackSpace: Boolean,
    onEmojiSelected: (String) -> Unit,
    onBackSpace: () -> Unit,
    isFilterFocused: MutableState<Boolean>
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imePadding = WindowInsets.ime.getBottom(density)

    val emojis = viewModel.emojis.collectAsState(initial = null)

    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 2)

    DisposableEffect(null) {
        onDispose {
            viewModel.filterEmojis("")
        }
    }

    LaunchedEffect(gridState.firstVisibleItemScrollOffset) {
        keyboardController?.hide()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if(emojis.value != null && showBackSpace) {
            Image(
                modifier = Modifier
                    .zIndex(1f)
                    .align(Alignment.BottomEnd)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 8.dp)
                    .scalingClickable {
                        onBackSpace()
                    }
                    .size(height = 42.dp, width = 64.dp)
                    .background(
                        color = LocalTheme.current.colors.brandMainDark,
                        shape = LocalTheme.current.shapes.rectangularActionShape
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .shadow(elevation = LocalTheme.current.styles.actionElevation),
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = stringResource(Res.string.accessibility_backspace),
                colorFilter = ColorFilter.tint(color = Colors.GrayLight)
            )
        }

        EmojiPicker(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp)
                .then(
                    if (imePadding > 0) Modifier else Modifier.animateContentSize()
                ),
            onEmojiSelected = onEmojiSelected,
            viewModel = viewModel,
            isFilterFocused = isFilterFocused,
            gridState = gridState
        )
    }
}