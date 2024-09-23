package components.pull_refresh

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chat.enrichment.shared.ui.base.LocalDeviceType
import chat.enrichment.shared.ui.components.getDefaultPullRefreshSize
import chat.enrichment.shared.ui.components.navigation.AppBarHeightDp

/**
 * Content which can be refresh by pulling down vertically.
 * Requires scrollable content, such as Modifier.verticalScroll, or LazyColumn.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RefreshableContent(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    content: @Composable (Modifier, indicatorOffset: Dp) -> Unit
) {
    val pullRefreshSize = getDefaultPullRefreshSize(
        isSmallDevice = LocalDeviceType.current == WindowWidthSizeClass.Compact
    )
    val indicatorOffset = remember { mutableStateOf(0.dp) }
    val indicatorWidth = remember { mutableStateOf(0f) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        refreshingOffset = pullRefreshSize.plus(AppBarHeightDp.dp),
        refreshThreshold = pullRefreshSize
    )

    DraggableRefreshIndicator(
        modifier = modifier
            .width(with(LocalDensity.current) {
                indicatorWidth.value.toDp()
            })
            .systemBarsPadding()
            .statusBarsPadding()
            .zIndex(100f),
        pullRefreshSize = pullRefreshSize,
        state = pullRefreshState,
        isRefreshing = true
    ) { indicatorOffsetDp ->
        indicatorOffset.value = indicatorOffsetDp
    }
    content(
        Modifier
            .graphicsLayer {
                indicatorWidth.value = size.width
            }
            .pullRefresh(pullRefreshState),
        indicatorOffset.value
    )
}