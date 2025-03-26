package augmy.interactive.shared.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect


@Composable
fun persistedLazyListState(
    persistentData: PersistentListData,
    onDispose: (PersistentListData) -> Unit
): LazyListState {
    println("kostka_test, persistedLazyListState: $persistentData")
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

data class PersistentListData(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemOffset: Int = 0
)
