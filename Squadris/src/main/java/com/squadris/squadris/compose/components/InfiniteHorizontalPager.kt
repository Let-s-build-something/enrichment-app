package com.squadris.squadris.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * A pager with ability to trick user into thinking we're moving around in cycles by duplicating last item as first and first as last.
 * We start on the second item (index of 1) and whenever we settle on the last one (copy of the second one), pager scrolls with no animation to the second one,
 * which gives the possibility to scroll forward and see item between second item and last yet again, the same way the other way around.
 *
 * This makes the pager bigger by 2 item and so there is a listener [onPageChanged] returning the corrected index
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteHorizontalPage(
    pageCount: Int,
    state: PagerState = rememberPagerState(
        initialPage = 1,
        initialPageOffsetFraction = 0f
    ) {
        pageCount
    },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = remember(state) {
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal)
    },
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onUserIntercepted: (interaction: Interaction) -> Unit = {},
    onUserRelease: (interaction: Interaction) -> Unit = {},
    onPageChanged: (page: Int) -> Unit = {},
    pageContent: @Composable (page: Int) -> Unit
) {
    HorizontalPager(
        state = state,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        pageContent = { page ->
            pageContent.invoke(
                when (page) {
                    0 -> pageCount.minus(1)
                    pageCount.plus(1) -> 0
                    else -> max(0, page.minus(1))
                }
            )
        }
    )

    //whenever we settle, check whether we should trick pager into thinking we moved into the front
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect {
                if(state.currentPage == pageCount + 1) {
                    coroutineScope.launch {
                        state.scrollToPage(1)
                    }
                }else if(state.currentPage == 0) {
                    coroutineScope.launch {
                        state.scrollToPage(pageCount)
                    }
                }
            }
    }
    // allow listen to interruptions and stops of interruptions
    LaunchedEffect(state) {
        state.interactionSource.interactions.collect { interaction ->
            when(interaction) {
                is PressInteraction.Press,
                is DragInteraction.Start -> onUserIntercepted.invoke(interaction)

                is PressInteraction.Release,
                is PressInteraction.Cancel,
                is DragInteraction.Cancel,
                is DragInteraction.Stop -> onUserRelease.invoke(interaction)
            }
        }
    }
    // return changed page with corrected index
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect {
            onPageChanged.invoke(
                when(state.currentPage) {
                    0 -> pageCount.minus(1)
                    pageCount.plus(1) -> 0
                    else -> it.minus(1)
                }
            )
        }
    }
}