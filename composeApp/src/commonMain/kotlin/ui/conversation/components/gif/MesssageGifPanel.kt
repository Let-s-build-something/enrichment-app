package ui.conversation.components.gif

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import data.io.social.network.conversation.giphy.GifAsset
import ui.conversation.ConversationModel

/** Panel for picking a gif to be sent in a message */
@Composable
fun MessageGifPanel(
    modifier: Modifier = Modifier,
    viewModel: ConversationModel,
    onGifSelected: (GifAsset) -> Unit,
    isFilterFocused: MutableState<Boolean>
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imePadding = WindowInsets.ime.getBottom(density)

    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState.firstVisibleItemScrollOffset) {
        keyboardController?.hide()
    }

    GifPicker(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 4.dp)
            .then(
                if (imePadding > 0) Modifier else Modifier.animateContentSize()
            ),
        onGifSelected = onGifSelected,
        viewModel = viewModel,
        gridState = gridState,
        isFilterFocused = isFilterFocused
    )
}