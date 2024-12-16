package ui.conversation.components.gif

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.interactive.shared.ui.base.LocalDeviceType
import base.utils.getOrNull
import data.io.social.network.conversation.giphy.GifAsset
import future_shared_module.ext.brandShimmerEffect
import future_shared_module.ext.scalingClickable
import ui.conversation.components.KeyboardViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Selector of giphy gifs */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun GifPicker(
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel,
    gridState: LazyStaggeredGridState,
    onGifSelected: (GifAsset) -> Unit
) {
    val density = LocalDensity.current

    val trendingGifs = viewModel.trendingGifs.collectAsState().value?.collectAsLazyPagingItems()

    val isLoadingInitialPage = trendingGifs?.loadState?.refresh is LoadState.Loading


    LaunchedEffect(Unit, isLoadingInitialPage) {
        if (viewModel.trendingGifs.value == null) {
            viewModel.requestTrendingGifs()
        }
    }

    LazyVerticalStaggeredGrid(
        modifier = modifier,
        columns = StaggeredGridCells.Fixed(
            when(LocalDeviceType.current) {
                WindowWidthSizeClass.Compact -> 2
                WindowWidthSizeClass.Medium -> 3
                WindowWidthSizeClass.Expanded -> 4
                else -> 2
            }
        ),
        state = gridState,
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(Modifier.height(16.dp))
        }
        items(
            count = if (trendingGifs?.itemCount == 0 && isLoadingInitialPage || trendingGifs == null) {
                SHIMMER_ITEM_COUNT
            } else trendingGifs.itemCount,
            key = { index ->
                trendingGifs?.getOrNull(index)?.uid ?: Uuid.random().toString()
            }
        ) { index ->
            trendingGifs?.getOrNull(index).let { data ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    val isLoaded = remember(data?.uid) { mutableStateOf(false) }
                    if(!isLoaded.value) {
                        val aspectRatio = remember(data?.uid) {
                            (6..14).random() / 10f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .brandShimmerEffect(shape = RoundedCornerShape(6.dp))
                                .aspectRatio(aspectRatio)
                        )
                    }

                    GifImage(
                        modifier = Modifier
                            .zIndex(1f)
                            .scalingClickable(scaleInto = .95f) {
                                onGifSelected(
                                    GifAsset(
                                        original = data?.images?.original?.url,
                                        fixedWidthOriginal = data?.images?.fixedWidth?.url,
                                        fixedWidthSmall = data?.images?.fixedWidthDownsampled?.url
                                    )
                                )
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .onSizeChanged {
                                if(it.height > 50) {
                                    isLoaded.value = true
                                }
                            },
                        url = data?.images?.fixedWidthDownsampled?.url ?: "",
                        contentDescription = data?.altText ?: data?.title,
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(Modifier.height(
                50.dp + with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
            ))
        }
    }
}

private const val SHIMMER_ITEM_COUNT = 20

/** Image displaying a GIF from an [url] */
@Composable
expect fun GifImage(
    modifier: Modifier = Modifier,
    url: String,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Fit
)
