package ui.conversation.media

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import augmy.interactive.shared.ext.detectTransformTwoDown
import augmy.interactive.shared.ui.base.LocalContentSize
import base.BrandBaseScreen
import base.navigation.NavIconType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ui.conversation.components.MediaElement

/**
 * Screen for displaying a list of media
 * @param urls list of remote urls to be displayed
 * @param selectedIndex initially selected index
 */
@Composable
fun MediaDetailScreen(
    title: String?,
    subtitle: String?,
    urls: Array<out String?>,
    selectedIndex: Int
) {
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { urls.size }
    )
    val currentIndex = rememberSaveable(urls, selectedIndex) {
        mutableStateOf(selectedIndex)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            currentIndex.value = it
        }
    }

    BrandBaseScreen(
        title = title + if(urls.size > 1) " | ${currentIndex.value + 1}/${urls.size}" else "",
        subtitle = subtitle,
        navIconType = NavIconType.CLOSE
    ) {
        val contentSize = LocalContentSize.current

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            beyondViewportPageCount = 1
        ) { index ->
            urls.getOrNull(index)?.let { url ->
                val offset = remember(index) {
                    mutableStateOf(Offset(0f, 0f))
                }
                val scale = remember(index) {
                    Animatable(initialValue = 1f)
                }

                MediaElement(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            translationX = offset.value.x
                            translationY = offset.value.y
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapCenter ->
                                    if (scale.value > 1f) {
                                        coroutineScope.launch {
                                            scale.animateTo(1f)
                                        }
                                        offset.value = Offset(0f, 0f)
                                    } else {
                                        coroutineScope.launch {
                                            scale.animateTo(2f)
                                        }
                                        val xDiff = contentSize.width - tapCenter.x
                                        val yDiff = contentSize.height - tapCenter.y
                                        offset.value = Offset(xDiff, yDiff)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformTwoDown(
                                isZoomed = {
                                    scale.value != 1f
                                },
                                onGesture = { _, panChange, zoomChange ->
                                    coroutineScope.launch {
                                        scale.animateTo(
                                            (scale.value * zoomChange * zoomChange).coerceIn(
                                                minimumValue = 1f,
                                                maximumValue = 3.5f
                                            )
                                        )
                                    }
                                    if(scale.value > 1f) {
                                        offset.value = Offset(
                                            x = (offset.value.x + panChange.x * scale.value).coerceIn(
                                                minimumValue = -contentSize.width * scale.value.minus(1),
                                                maximumValue = contentSize.width * scale.value.minus(1)
                                            ),
                                            y = (offset.value.y + panChange.y * scale.value).coerceIn(
                                                minimumValue = -contentSize.height * scale.value.minus(1),
                                                maximumValue = contentSize.height * scale.value.minus(1)
                                            )
                                        )
                                    }
                                }
                            )
                        },
                    contentScale = ContentScale.Fit,
                    url = url
                )
            }
        }
    }
}
