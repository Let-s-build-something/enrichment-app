package ui.conversation.components.gif

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_giphy_logo
import augmy.composeapp.generated.resources.action_search_gifs
import augmy.interactive.shared.ext.brandShimmerEffect
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.components.input.DELAY_BETWEEN_TYPING_SHORT
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.utils.getOrNull
import data.io.social.network.conversation.giphy.GifAsset
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
    isFilterFocused: MutableState<Boolean>,
    onGifSelected: (GifAsset) -> Unit
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val searchCoroutineScope = rememberCoroutineScope()

    val filterQuery = remember(density) {
        mutableStateOf(TextFieldValue())
    }

    val gifs = (if(filterQuery.value.text.isBlank()) {
        viewModel.trendingGifs
    }else viewModel.queriedGifs).collectAsState().value?.collectAsLazyPagingItems()
    val isLoadingInitialPage = gifs?.loadState?.refresh is LoadState.Loading


    LaunchedEffect(Unit, isLoadingInitialPage) {
        if (viewModel.trendingGifs.value == null) {
            viewModel.requestTrendingGifs()
        }
    }

    DisposableEffect(searchCoroutineScope) {
        onDispose {
            focusManager.clearFocus()
            isFilterFocused.value = false
        }
    }


    Column(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        }
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = LocalTheme.current.colors.backgroundLight),
            visible = (isFilterFocused.value || filterQuery.value.text.isNotBlank())
                    && gridState.firstVisibleItemIndex > 0
        ) {
            Image(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                painter = painterResource(LocalTheme.current.icons.giphy),
                contentDescription = stringResource(Res.string.accessibility_giphy_logo),
                contentScale = ContentScale.FillHeight
            )
        }
        LazyVerticalStaggeredGrid(
            modifier = Modifier.weight(1f),
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EditFieldInput(
                        modifier = Modifier
                            .requiredHeight(44.dp)
                            .fillMaxWidth(.65f)
                            .onFocusChanged {
                                isFilterFocused.value = it.isFocused
                            },
                        value = "",
                        shape = LocalTheme.current.shapes.circularActionShape,
                        hint = stringResource(Res.string.action_search_gifs),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        paddingValues = PaddingValues(start = 16.dp),
                        minHeight = 38.dp,
                        leadingIcon = Icons.Outlined.Search,
                        onValueChange = {
                            filterQuery.value = it
                            searchCoroutineScope.coroutineContext.cancelChildren()
                            searchCoroutineScope.launch {
                                delay(DELAY_BETWEEN_TYPING_SHORT)
                                viewModel.requestGifSearch(it.text)
                            }
                        },
                        onClear = {
                            filterQuery.value = TextFieldValue()
                            focusManager.clearFocus()
                        },
                        isClearable = true,
                        colors = LocalTheme.current.styles.textFieldColors.copy(
                            focusedTextColor = LocalTheme.current.colors.disabled,
                            unfocusedTextColor = LocalTheme.current.colors.disabled,
                            focusedContainerColor = LocalTheme.current.colors.backgroundDark,
                            unfocusedContainerColor = LocalTheme.current.colors.backgroundDark
                        )
                    )
                }
            }
            items(
                count = if (gifs?.itemCount == 0 && isLoadingInitialPage || gifs == null) {
                    SHIMMER_ITEM_COUNT
                } else gifs.itemCount,
                key = { index ->
                    gifs?.getOrNull(index)?.uid ?: Uuid.random().toString()
                }
            ) { index ->
                gifs?.getOrNull(index).let { data ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .animateItem()
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
                                    filterQuery.value = TextFieldValue()
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
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = LocalTheme.current.colors.backgroundLight),
            visible = (isFilterFocused.value || filterQuery.value.text.isNotBlank())
                    && gridState.firstVisibleItemIndex == 0
        ) {
            Image(
                modifier = Modifier
                    .navigationBarsPadding()
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(LocalTheme.current.icons.giphy),
                contentDescription = stringResource(Res.string.accessibility_giphy_logo),
                contentScale = ContentScale.FillHeight
            )
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
