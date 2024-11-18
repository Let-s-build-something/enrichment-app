package components.pull_refresh

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.components.getDefaultPullRefreshSize
import augmy.interactive.shared.ui.components.navigation.AppBarHeightDp
import base.BrandBaseScreen
import base.navigation.NavIconType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Implementation of the [BrandBaseScreen] with pull to refresh logic
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RefreshableScreen(
    modifier: Modifier = Modifier,
    viewModel: RefreshableViewModel,
    navIconType: NavIconType? = null,
    title: String? = null,
    subtitle: String? = null,
    onBackPressed: () -> Boolean = { true },
    onRefresh: () -> Unit = {},
    actionIcons: (@Composable (expanded: Boolean) -> Unit)? = null,
    showDefaultActions: Boolean = actionIcons == null,
    onNavigationIconClick: (() -> Unit)? = null,
    appBarVisible: Boolean = true,
    contentColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val pullRefreshSize = getDefaultPullRefreshSize(
        isSmallDevice = LocalDeviceType.current == WindowWidthSizeClass.Compact
    )
    val indicatorOffset = remember { mutableStateOf(0.dp) }
    val indicatorWidth = remember { mutableStateOf(0f) }

    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val refreshMe = {
        onRefresh()
        viewModel.requestData(
            scope = CoroutineScope(Dispatchers.Default),
            isSpecial = true,
            isPullRefresh = true
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing.value,
        onRefresh = refreshMe,
        refreshingOffset = pullRefreshSize.plus(AppBarHeightDp.dp),
        refreshThreshold = pullRefreshSize
    )

    DraggableRefreshIndicator(
        modifier = Modifier
            .width(with(LocalDensity.current) {
                indicatorWidth.value.toDp()
            })
            .zIndex(100f),
        pullRefreshSize = pullRefreshSize,
        state = pullRefreshState,
        isRefreshing = isRefreshing.value
    ) { indicatorOffsetDp ->
        indicatorOffset.value = indicatorOffsetDp
    }

    CompositionLocalProvider(
        LocalRefreshCallback provides refreshMe
    ) {
        BrandBaseScreen(
            modifier = modifier,
            navIconType = navIconType,
            title = title,
            subtitle = subtitle,
            onBackPressed = onBackPressed,
            actionIcons = actionIcons,
            showDefaultActions = showDefaultActions,
            contentModifier = Modifier
                .graphicsLayer {
                    translationY = indicatorOffset.value
                        .roundToPx()
                        .toFloat()
                    indicatorWidth.value = size.width
                }
                .then(
                    if(currentPlatform == PlatformType.Jvm) Modifier
                    else Modifier.pullRefresh(pullRefreshState)
                ),
            content = content,
            onNavigationIconClick = onNavigationIconClick,
            appBarVisible = appBarVisible,
            contentColor = contentColor,
            floatingActionButtonPosition = floatingActionButtonPosition,
            floatingActionButton = floatingActionButton
        )
    }
}

/** Callback for refreshing content */
val LocalRefreshCallback = staticCompositionLocalOf<(() -> Unit)?> { null }
