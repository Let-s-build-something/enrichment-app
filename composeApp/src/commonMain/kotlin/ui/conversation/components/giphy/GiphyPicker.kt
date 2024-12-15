package ui.conversation.components.giphy

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/** Selector of giphy gifs */
@Composable
fun GiphyPicker(
    modifier: Modifier = Modifier,
    viewModel: GiphyViewModel = koinViewModel()
) {
    val density = LocalDensity.current

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 38.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(16.dp))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(
                50.dp + with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
            ))
        }
    }
}