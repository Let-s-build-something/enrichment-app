package ui.conversation.components.gif

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import data.io.social.network.conversation.giphy.GifAsset
import kotlinx.coroutines.delay
import ui.conversation.ConversationViewModel

/** Panel for picking a gif to be sent in a message */
@Composable
fun MessageGifPanel(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel,
    onGifSelected: (GifAsset) -> Unit,
    onDismissRequest: () -> Unit
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeHeight = viewModel.keyboardHeight
    val imePadding = WindowInsets.ime.getBottom(density)

    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(imePadding) {
        delay(100)
        if(imeHeight - imePadding <= 0) {
            onDismissRequest()
        }
    }

    LaunchedEffect(gridState.firstVisibleItemScrollOffset) {
        keyboardController?.hide()
    }

    Box(
        modifier = modifier
            .padding(
                bottom = with(density) {
                    viewModel.additionalBottomPadding.value.toDp()
                }
            )
            .animateContentSize()
    ) {
        GifPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { imeHeight.toDp() })
                .padding(horizontal = 4.dp)
                .then(
                    if (imePadding > 0) Modifier else Modifier.animateContentSize()
                ),
            onGifSelected = onGifSelected,
            viewModel = viewModel,
            gridState = gridState
        )
    }
}