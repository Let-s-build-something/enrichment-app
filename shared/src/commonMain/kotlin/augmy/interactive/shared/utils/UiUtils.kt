package augmy.interactive.shared.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect


@Composable
fun persistedLazyListState(
    persistentData: PersistentListData,
    onDispose: (PersistentListData) -> Unit
): LazyListState {
    val scrollState = rememberLazyListState(
        persistentData.firstVisibleItemIndex,
        persistentData.firstVisibleItemOffset
    )

    DisposableEffect(key1 = null) {
        onDispose {
            onDispose(
                PersistentListData(
                    scrollState.firstVisibleItemIndex,
                    scrollState.firstVisibleItemScrollOffset
                )
            )
        }
    }
    return scrollState
}

@Composable
fun persistedLazyGridState(
    persistentData: PersistentListData,
    onDispose: (PersistentListData) -> Unit
): LazyGridState {
    val scrollState = rememberLazyGridState(
        persistentData.firstVisibleItemIndex,
        persistentData.firstVisibleItemOffset
    )

    DisposableEffect(key1 = null) {
        onDispose {
            onDispose(
                PersistentListData(
                    scrollState.firstVisibleItemIndex,
                    scrollState.firstVisibleItemScrollOffset
                )
            )
        }
    }
    return scrollState
}

data class PersistentListData(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemOffset: Int = 0
)
