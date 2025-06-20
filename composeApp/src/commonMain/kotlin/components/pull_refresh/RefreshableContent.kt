package components.pull_refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.LocalIsMouseUser
import augmy.interactive.shared.ui.components.getDefaultPullRefreshSize
import augmy.interactive.shared.ui.components.navigation.AppBarHeightDp

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
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
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

    Box {
        DraggableRefreshIndicator(
            modifier = Modifier
                .width(with(LocalDensity.current) {
                    indicatorWidth.value.toDp()
                })
                .zIndex(100f),
            pullRefreshSize = pullRefreshSize,
            state = pullRefreshState,
            isRefreshing = isRefreshing
        ) { indicatorOffsetDp ->
            indicatorOffset.value = indicatorOffsetDp
        }
        Box(
            modifier = modifier
                .graphicsLayer {
                    translationY = indicatorOffset.value
                        .roundToPx()
                        .toFloat()
                    indicatorWidth.value = size.width
                }
                .then(
                    if(LocalIsMouseUser.current) Modifier
                    else Modifier.pullRefresh(pullRefreshState)
                ),
            content = content,
            contentAlignment = contentAlignment
        )
    }
}