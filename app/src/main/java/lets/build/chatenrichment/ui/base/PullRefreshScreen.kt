package lets.build.chatenrichment.ui.base

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.squadris.squadris.compose.components.getDefaultPullRefreshSize
import com.squadris.squadris.compose.base.LocalIsTablet
import com.squadris.squadris.compose.components.DraggableRefreshIndicator
import com.squadris.squadris.compose.components.navigation.AppBarHeightDp
import com.squadris.squadris.compose.components.navigation.NavIconType
import com.squadris.squadris.compose.components.collapsing_layout.rememberCollapsingLayout
import com.squadris.squadris.utils.RefreshableViewModel

/**
 * Implementation of the [BrandBaseScreen] with pull to refresh logic
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullRefreshScreen(
    modifier: Modifier = Modifier,
    viewModel: RefreshableViewModel,
    navIconType: NavIconType = NavIconType.BACK,
    title: String? = null,
    subtitle: String? = null,
    onBackPressed: () -> Boolean = { true },
    actionIcons: (@Composable RowScope.() -> Unit)? = null,
    appBarVisible: Boolean = true,
    onNavigationIconClick: (() -> Unit)? = null,
    contentColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val refreshScope = rememberCoroutineScope()
    val isTablet = LocalIsTablet.current
    val pullRefreshSize = getDefaultPullRefreshSize(isTablet = isTablet)
    val indicatorOffset = remember { mutableStateOf(0.dp) }

    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing.value,
        onRefresh = {
            viewModel.requestData(scope = refreshScope, isSpecial = true, isPullRefresh = true)
        },
        refreshingOffset = pullRefreshSize.plus(AppBarHeightDp.dp),
        refreshThreshold = pullRefreshSize
    )
    val collapsingLayoutState = rememberCollapsingLayout()

    LaunchedEffect(indicatorOffset.value) {
        collapsingLayoutState.isEnabled.value = indicatorOffset.value == 0.dp
    }

    DraggableRefreshIndicator(
        modifier = Modifier
            .systemBarsPadding()
            .statusBarsPadding(),
        pullRefreshSize = pullRefreshSize,
        state = pullRefreshState,
        isRefreshing = isRefreshing.value
    ) { indicatorOffsetDp ->
        indicatorOffset.value = indicatorOffsetDp
    }

    BrandBaseScreen(
        modifier = modifier,
        navIconType = navIconType,
        title = title,
        collapsingLayoutState = collapsingLayoutState,
        subtitle = subtitle,
        onBackPressed = onBackPressed,
        actionIcons = actionIcons,
        contentModifier = Modifier
            .graphicsLayer {
                translationY = indicatorOffset.value
                    .roundToPx()
                    .toFloat()
            }
            .pullRefresh(pullRefreshState),
        content = content,
        onNavigationIconClick = onNavigationIconClick,
        appBarVisible = appBarVisible,
        contentColor = contentColor,
        floatingActionButtonPosition = floatingActionButtonPosition,
        floatingActionButton = floatingActionButton
    )
}